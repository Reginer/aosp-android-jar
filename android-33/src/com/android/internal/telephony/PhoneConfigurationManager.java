/*
 * Copyright (C) 2018 The Android Open Source Project
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

import static android.telephony.TelephonyManager.ACTION_MULTI_SIM_CONFIG_CHANGED;
import static android.telephony.TelephonyManager.EXTRA_ACTIVE_SIM_SUPPORTED_COUNT;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.sysprop.TelephonyProperties;
import android.telephony.PhoneCapability;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * This class manages phone's configuration which defines the potential capability (static) of the
 * phone and its current activated capability (current).
 * It gets and monitors static and current phone capability from the modem; send broadcast
 * if they change, and and sends commands to modem to enable or disable phones.
 */
public class PhoneConfigurationManager {
    public static final String DSDA = "dsda";
    public static final String DSDS = "dsds";
    public static final String TSTS = "tsts";
    public static final String SSSS = "";
    private static final String LOG_TAG = "PhoneCfgMgr";
    private static final int EVENT_SWITCH_DSDS_CONFIG_DONE = 100;
    private static final int EVENT_GET_MODEM_STATUS = 101;
    private static final int EVENT_GET_MODEM_STATUS_DONE = 102;
    private static final int EVENT_GET_PHONE_CAPABILITY_DONE = 103;

    private static PhoneConfigurationManager sInstance = null;
    private final Context mContext;
    private PhoneCapability mStaticCapability;
    private final RadioConfig mRadioConfig;
    private final Handler mHandler;
    // mPhones is obtained from PhoneFactory and can have phones corresponding to inactive modems as
    // well. That is, the array size can be 2 even if num of active modems is 1.
    private Phone[] mPhones;
    private final Map<Integer, Boolean> mPhoneStatusMap;
    private MockableInterface mMi = new MockableInterface();
    private TelephonyManager mTelephonyManager;
    private static final RegistrantList sMultiSimConfigChangeRegistrants = new RegistrantList();
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);
    /**
     * Init method to instantiate the object
     * Should only be called once.
     */
    public static PhoneConfigurationManager init(Context context) {
        synchronized (PhoneConfigurationManager.class) {
            if (sInstance == null) {
                sInstance = new PhoneConfigurationManager(context);
            } else {
                Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
            }
            return sInstance;
        }
    }

    /**
     * Constructor.
     * @param context context needed to send broadcast.
     */
    private PhoneConfigurationManager(Context context) {
        mContext = context;
        // TODO: send commands to modem once interface is ready.
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        //initialize with default, it'll get updated when RADIO is ON/AVAILABLE
        mStaticCapability = getDefaultCapability();
        mRadioConfig = RadioConfig.getInstance();
        mHandler = new ConfigManagerHandler();
        mPhoneStatusMap = new HashMap<>();

        notifyCapabilityChanged();

        mPhones = PhoneFactory.getPhones();

        for (Phone phone : mPhones) {
            registerForRadioState(phone);
        }
    }

    private void registerForRadioState(Phone phone) {
        phone.mCi.registerForAvailable(mHandler, Phone.EVENT_RADIO_AVAILABLE, phone);
    }

    private PhoneCapability getDefaultCapability() {
        if (getPhoneCount() > 1) {
            return PhoneCapability.DEFAULT_DSDS_CAPABILITY;
        } else {
            return PhoneCapability.DEFAULT_SSSS_CAPABILITY;
        }
    }

    /**
     * Static method to get instance.
     */
    public static PhoneConfigurationManager getInstance() {
        if (sInstance == null) {
            Log.wtf(LOG_TAG, "getInstance null");
        }

        return sInstance;
    }

    /**
     * Handler class to handle callbacks
     */
    private final class ConfigManagerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            AsyncResult ar;
            Phone phone = null;
            switch (msg.what) {
                case Phone.EVENT_RADIO_AVAILABLE:
                case Phone.EVENT_RADIO_ON:
                    log("Received EVENT_RADIO_AVAILABLE/EVENT_RADIO_ON");
                    ar = (AsyncResult) msg.obj;
                    if (ar.userObj != null && ar.userObj instanceof Phone) {
                        phone = (Phone) ar.userObj;
                        updatePhoneStatus(phone);
                    } else {
                        // phone is null
                        log("Unable to add phoneStatus to cache. "
                                + "No phone object provided for event " + msg.what);
                    }
                    getStaticPhoneCapability();
                    break;
                case EVENT_SWITCH_DSDS_CONFIG_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null && ar.exception == null) {
                        int numOfLiveModems = msg.arg1;
                        onMultiSimConfigChanged(numOfLiveModems);
                    } else {
                        log(msg.what + " failure. Not switching multi-sim config." + ar.exception);
                    }
                    break;
                case EVENT_GET_MODEM_STATUS_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null && ar.exception == null) {
                        int phoneId = msg.arg1;
                        boolean enabled = (boolean) ar.result;
                        // update the cache each time getModemStatus is requested
                        addToPhoneStatusCache(phoneId, enabled);
                    } else {
                        log(msg.what + " failure. Not updating modem status." + ar.exception);
                    }
                    break;
                case EVENT_GET_PHONE_CAPABILITY_DONE:
                    ar = (AsyncResult) msg.obj;
                    if (ar != null && ar.exception == null) {
                        mStaticCapability = (PhoneCapability) ar.result;
                        notifyCapabilityChanged();
                    } else {
                        log(msg.what + " failure. Not getting phone capability." + ar.exception);
                    }
                    break;
            }
        }
    }

    /**
     * Enable or disable phone
     *
     * @param phone which phone to operate on
     * @param enable true or false
     * @param result the message to sent back when it's done.
     */
    public void enablePhone(Phone phone, boolean enable, Message result) {
        if (phone == null) {
            log("enablePhone failed phone is null");
            return;
        }
        phone.mCi.enableModem(enable, result);
    }

    /**
     * Get phone status (enabled/disabled)
     * first query cache, if the status is not in cache,
     * add it to cache and return a default value true (non-blocking).
     *
     * @param phone which phone to operate on
     */
    public boolean getPhoneStatus(Phone phone) {
        if (phone == null) {
            log("getPhoneStatus failed phone is null");
            return false;
        }

        int phoneId = phone.getPhoneId();

        //use cache if the status has already been updated/queried
        try {
            return getPhoneStatusFromCache(phoneId);
        } catch (NoSuchElementException ex) {
            // Return true if modem status cannot be retrieved. For most cases, modem status
            // is on. And for older version modems, GET_MODEM_STATUS and disable modem are not
            // supported. Modem is always on.
            //TODO: this should be fixed in R to support a third status UNKNOWN b/131631629
            return true;
        } finally {
            //in either case send an asynchronous request to retrieve the phone status
            updatePhoneStatus(phone);
        }
    }

    /**
     * Get phone status (enabled/disabled) directly from modem, and use a result Message object
     * Note: the caller of this method is reponsible to call this in a blocking fashion as well
     * as read the results and handle the error case.
     * (In order to be consistent, in error case, we should return default value of true; refer
     *  to #getPhoneStatus method)
     *
     * @param phone which phone to operate on
     * @param result message that will be updated with result
     */
    public void getPhoneStatusFromModem(Phone phone, Message result) {
        if (phone == null) {
            log("getPhoneStatus failed phone is null");
        }
        phone.mCi.getModemStatus(result);
    }

    /**
     * return modem status from cache, NoSuchElementException if phoneId not in cache
     * @param phoneId
     */
    public boolean getPhoneStatusFromCache(int phoneId) throws NoSuchElementException {
        if (mPhoneStatusMap.containsKey(phoneId)) {
            return mPhoneStatusMap.get(phoneId);
        } else {
            throw new NoSuchElementException("phoneId not found: " + phoneId);
        }
    }

    /**
     * method to call RIL getModemStatus
     */
    private void updatePhoneStatus(Phone phone) {
        Message result = Message.obtain(
                mHandler, EVENT_GET_MODEM_STATUS_DONE, phone.getPhoneId(), 0 /**dummy arg*/);
        phone.mCi.getModemStatus(result);
    }

    /**
     * Add status of the phone to the status HashMap
     * @param phoneId
     * @param status
     */
    public void addToPhoneStatusCache(int phoneId, boolean status) {
        mPhoneStatusMap.put(phoneId, status);
    }

    /**
     * Returns how many phone objects the device supports.
     */
    public int getPhoneCount() {
        return mTelephonyManager.getActiveModemCount();
    }

    /**
     * get static overall phone capabilities for all phones.
     */
    public synchronized PhoneCapability getStaticPhoneCapability() {
        if (getDefaultCapability().equals(mStaticCapability)) {
            log("getStaticPhoneCapability: sending the request for getting PhoneCapability");
            Message callback = Message.obtain(
                    mHandler, EVENT_GET_PHONE_CAPABILITY_DONE);
            mRadioConfig.getPhoneCapability(callback);
        }
        log("getStaticPhoneCapability: mStaticCapability " + mStaticCapability);
        return mStaticCapability;
    }

    /**
     * get configuration related status of each phone.
     */
    public PhoneCapability getCurrentPhoneCapability() {
        return getStaticPhoneCapability();
    }

    public int getNumberOfModemsWithSimultaneousDataConnections() {
        return mStaticCapability.getMaxActiveDataSubscriptions();
    }

    private void notifyCapabilityChanged() {
        PhoneNotifier notifier = new DefaultPhoneNotifier(mContext);

        notifier.notifyPhoneCapabilityChanged(mStaticCapability);
    }

    /**
     * Switch configs to enable multi-sim or switch back to single-sim
     * @param numOfSims number of active sims we want to switch to
     */
    public void switchMultiSimConfig(int numOfSims) {
        log("switchMultiSimConfig: with numOfSims = " + numOfSims);
        if (getStaticPhoneCapability().getLogicalModemList().size() < numOfSims) {
            log("switchMultiSimConfig: Phone is not capable of enabling "
                    + numOfSims + " sims, exiting!");
            return;
        }
        if (getPhoneCount() != numOfSims) {
            log("switchMultiSimConfig: sending the request for switching");
            Message callback = Message.obtain(
                    mHandler, EVENT_SWITCH_DSDS_CONFIG_DONE, numOfSims, 0 /**dummy arg*/);
            mRadioConfig.setNumOfLiveModems(numOfSims, callback);
        } else {
            log("switchMultiSimConfig: No need to switch. getNumOfActiveSims is already "
                    + numOfSims);
        }
    }

    /**
     * Get whether reboot is required or not after making changes to modem configurations.
     * Return value defaults to true
     */
    public boolean isRebootRequiredForModemConfigChange() {
        return mMi.isRebootRequiredForModemConfigChange();
    }

    private void onMultiSimConfigChanged(int numOfActiveModems) {
        int oldNumOfActiveModems = getPhoneCount();
        setMultiSimProperties(numOfActiveModems);

        if (isRebootRequiredForModemConfigChange()) {
            log("onMultiSimConfigChanged: Rebooting.");
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            pm.reboot("Multi-SIM config changed.");
        } else {
            log("onMultiSimConfigChanged: Rebooting is not required.");
            mMi.notifyPhoneFactoryOnMultiSimConfigChanged(mContext, numOfActiveModems);
            broadcastMultiSimConfigChange(numOfActiveModems);
            boolean subInfoCleared = false;
            // if numOfActiveModems is decreasing, deregister old RILs
            // eg if we are going from 2 phones to 1 phone, we need to deregister RIL for the
            // second phone. This loop does nothing if numOfActiveModems is increasing.
            for (int phoneId = numOfActiveModems; phoneId < oldNumOfActiveModems; phoneId++) {
                SubscriptionController.getInstance().clearSubInfoRecord(phoneId);
                subInfoCleared = true;
                mPhones[phoneId].mCi.onSlotActiveStatusChange(
                        SubscriptionManager.isValidPhoneId(phoneId));
            }
            if (subInfoCleared) {
                // This triggers update of default subs. This should be done asap after
                // setMultiSimProperties() to avoid (minimize) duration for which default sub can be
                // invalid and can map to a non-existent phone.
                // If forexample someone calls a TelephonyManager API on default sub after
                // setMultiSimProperties() and before onSubscriptionsChanged() below -- they can be
                // using an invalid sub, which can map to a non-existent phone and can cause an
                // exception (see b/163582235).
                MultiSimSettingController.getInstance().onPhoneRemoved();
            }
            // old phone objects are not needed now; mPhones can be updated
            mPhones = PhoneFactory.getPhones();
            // if numOfActiveModems is increasing, register new RILs
            // eg if we are going from 1 phone to 2 phones, we need to register RIL for the second
            // phone. This loop does nothing if numOfActiveModems is decreasing.
            for (int phoneId = oldNumOfActiveModems; phoneId < numOfActiveModems; phoneId++) {
                Phone phone = mPhones[phoneId];
                registerForRadioState(phone);
                phone.mCi.onSlotActiveStatusChange(SubscriptionManager.isValidPhoneId(phoneId));
            }

            // When the user enables DSDS mode, the default VOICE and SMS subId should be switched
            // to "No Preference".  Doing so will sync the network/sim settings and telephony.
            // (see b/198123192)
            if (numOfActiveModems > oldNumOfActiveModems && numOfActiveModems == 2) {
                Log.i(LOG_TAG, " onMultiSimConfigChanged: DSDS mode enabled; "
                        + "setting VOICE & SMS subId to -1 (No Preference)");

                //Set the default VOICE subId to -1 ("No Preference")
                SubscriptionController.getInstance().setDefaultVoiceSubId(
                        SubscriptionManager.INVALID_SUBSCRIPTION_ID);

                //TODO:: Set the default SMS sub to "No Preference". Tracking this bug (b/227386042)
            } else {
                Log.i(LOG_TAG,
                        "onMultiSimConfigChanged: DSDS mode NOT detected.  NOT setting the "
                                + "default VOICE and SMS subId to -1 (No Preference)");
            }
        }
    }

    /**
     * Helper method to set system properties for setting multi sim configs,
     * as well as doing the phone reboot
     * NOTE: In order to support more than 3 sims, we need to change this method.
     * @param numOfActiveModems number of active sims
     */
    private void setMultiSimProperties(int numOfActiveModems) {
        mMi.setMultiSimProperties(numOfActiveModems);
    }

    @VisibleForTesting
    public static void notifyMultiSimConfigChange(int numOfActiveModems) {
        sMultiSimConfigChangeRegistrants.notifyResult(numOfActiveModems);
    }

    /**
     * Register for multi-SIM configuration change, for example if the devices switched from single
     * SIM to dual-SIM mode.
     *
     * It doesn't trigger callback upon registration as multi-SIM config change is in-frequent.
     */
    public static void registerForMultiSimConfigChange(Handler h, int what, Object obj) {
        sMultiSimConfigChangeRegistrants.addUnique(h, what, obj);
    }

    /**
     * Unregister for multi-SIM configuration change.
     */
    public static void unregisterForMultiSimConfigChange(Handler h) {
        sMultiSimConfigChangeRegistrants.remove(h);
    }

    /**
     * Unregister for all multi-SIM configuration change events.
     */
    public static void unregisterAllMultiSimConfigChangeRegistrants() {
        sMultiSimConfigChangeRegistrants.removeAll();
    }

    private void broadcastMultiSimConfigChange(int numOfActiveModems) {
        log("broadcastSimSlotNumChange numOfActiveModems" + numOfActiveModems);
        // Notify internal registrants first.
        notifyMultiSimConfigChange(numOfActiveModems);

        Intent intent = new Intent(ACTION_MULTI_SIM_CONFIG_CHANGED);
        intent.putExtra(EXTRA_ACTIVE_SIM_SUPPORTED_COUNT, numOfActiveModems);
        mContext.sendBroadcast(intent);
    }
    /**
     * This is invoked from shell commands during CTS testing only.
     * @return true if the modem service is set successfully, false otherwise.
     */
    public boolean setModemService(String serviceName) {
        if (mRadioConfig == null || mPhones[0] == null) {
            return false;
        }

        log("setModemService: " + serviceName);
        boolean statusRadioConfig = false;
        boolean statusRil = false;
        final boolean isAllowed = SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false);

        // Check for ALLOW_MOCK_MODEM_PROPERTY on user builds
        if (isAllowed || DEBUG) {
            if (serviceName != null) {
                statusRadioConfig = mRadioConfig.setModemService(serviceName);

                //TODO: consider multi-sim case (b/210073692)
                statusRil = mPhones[0].mCi.setModemService(serviceName);
            } else {
                statusRadioConfig = mRadioConfig.setModemService(null);

                //TODO: consider multi-sim case
                statusRil = mPhones[0].mCi.setModemService(null);
            }

            return statusRadioConfig && statusRil;
        } else {
            loge("setModemService is not allowed");
            return false;
        }
    }

     /**
     * This is invoked from shell commands to query during CTS testing only.
     * @return the service name of the connected service.
     */
    public String getModemService() {
        //TODO: consider multi-sim case
        if (mPhones[0] == null) {
            return "";
        }

        return mPhones[0].mCi.getModemService();
    }

    /**
     * A wrapper class that wraps some methods so that they can be replaced or mocked in unit-tests.
     *
     * For example, setting or reading system property are static native methods that can't be
     * directly mocked. We can mock it by replacing MockableInterface object with a mock instance
     * in unittest.
     */
    @VisibleForTesting
    public static class MockableInterface {
        /**
         * Wrapper function to decide whether reboot is required for modem config change.
         */
        @VisibleForTesting
        public boolean isRebootRequiredForModemConfigChange() {
            boolean rebootRequired = TelephonyProperties.reboot_on_modem_change().orElse(false);
            log("isRebootRequiredForModemConfigChange: isRebootRequired = " + rebootRequired);
            return rebootRequired;
        }

        /**
         * Wrapper function to call setMultiSimProperties.
         */
        @VisibleForTesting
        public void setMultiSimProperties(int numOfActiveModems) {
            String multiSimConfig;
            switch(numOfActiveModems) {
                case 3:
                    multiSimConfig = TSTS;
                    break;
                case 2:
                    multiSimConfig = DSDS;
                    break;
                default:
                    multiSimConfig = SSSS;
            }

            log("setMultiSimProperties to " + multiSimConfig);
            TelephonyProperties.multi_sim_config(multiSimConfig);
        }

        /**
         * Wrapper function to call PhoneFactory.onMultiSimConfigChanged.
         */
        @VisibleForTesting
        public void notifyPhoneFactoryOnMultiSimConfigChanged(
                Context context, int numOfActiveModems) {
            PhoneFactory.onMultiSimConfigChanged(context, numOfActiveModems);
        }
    }

    private static void log(String s) {
        Rlog.d(LOG_TAG, s);
    }

    private static void loge(String s) {
        Rlog.e(LOG_TAG, s);
    }

    private static void loge(String s, Exception ex) {
        Rlog.e(LOG_TAG, s, ex);
    }
}
