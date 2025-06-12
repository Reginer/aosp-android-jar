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

import android.annotation.NonNull;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncResult;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.RegistrantList;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.DeviceConfig;
import android.sysprop.TelephonyProperties;
import android.telephony.PhoneCapability;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.TelephonyRegistryManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.subscription.SubscriptionManagerService;
import com.android.telephony.Rlog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class manages phone's configuration which defines the potential capability (static) of the
 * phone and its current activated capability (current).
 * It gets and monitors static and current phone capability from the modem; send broadcast
 * if they change, and sends commands to modem to enable or disable phones.
 */
public class PhoneConfigurationManager {
    public static final String DSDA = "dsda";
    public static final String DSDS = "dsds";
    public static final String TSTS = "tsts";
    public static final String SSSS = "";
    /** DeviceConfig key for whether Virtual DSDA is enabled. */
    private static final String KEY_ENABLE_VIRTUAL_DSDA = "enable_virtual_dsda";
    private static final String LOG_TAG = "PhoneCfgMgr";
    private static final int EVENT_SWITCH_DSDS_CONFIG_DONE = 100;
    private static final int EVENT_GET_MODEM_STATUS = 101;
    private static final int EVENT_GET_MODEM_STATUS_DONE = 102;
    private static final int EVENT_GET_PHONE_CAPABILITY_DONE = 103;
    private static final int EVENT_DEVICE_CONFIG_CHANGED = 104;
    private static final int EVENT_GET_SIMULTANEOUS_CALLING_SUPPORT_DONE = 105;
    private static final int EVENT_SIMULTANEOUS_CALLING_SUPPORT_CHANGED = 106;

    /**
     * Listener interface for events related to the {@link PhoneConfigurationManager} which should
     * be reported to the {@link SimultaneousCallingTracker}.
     */
    public interface Listener {
        public void onPhoneCapabilityChanged();
        public void onDeviceConfigChanged();
    }

    /**
     * Base listener implementation.
     */
    public abstract static class ListenerBase implements Listener {
        @Override
        public void onPhoneCapabilityChanged() {}
        @Override
        public void onDeviceConfigChanged() {}
    }


    private static PhoneConfigurationManager sInstance = null;
    private final Context mContext;
    // Static capability retrieved from the modem - may be null in the case where no info has been
    // retrieved yet.
    private PhoneCapability mStaticCapability = null;
    private final Set<Integer> mSlotsSupportingSimultaneousCellularCalls = new HashSet<>(3);
    private final Set<Integer> mSubIdsSupportingSimultaneousCellularCalls = new HashSet<>(3);
    private final HashSet<Consumer<Set<Integer>>> mSimultaneousCellularCallingListeners =
            new HashSet<>(1);
    private final RadioConfig mRadioConfig;
    private final Handler mHandler;
    // mPhones is obtained from PhoneFactory and can have phones corresponding to inactive modems as
    // well. That is, the array size can be 2 even if num of active modems is 1.
    private Phone[] mPhones;
    private final Map<Integer, Boolean> mPhoneStatusMap;
    private MockableInterface mMi = new MockableInterface();
    private TelephonyManager mTelephonyManager;

    /** Feature flags */
    @NonNull
    private final FeatureFlags mFeatureFlags;
    private final DefaultPhoneNotifier mNotifier;
    public Set<Listener> mListeners = new CopyOnWriteArraySet<>();

    /**
     * True if 'Virtual DSDA' i.e., in-call IMS connectivity on both subs with only single logical
     * modem, is enabled.
     */
    private boolean mVirtualDsdaEnabled;
    private static final RegistrantList sMultiSimConfigChangeRegistrants = new RegistrantList();
    private static final String ALLOW_MOCK_MODEM_PROPERTY = "persist.radio.allow_mock_modem";
    private static final String BOOT_ALLOW_MOCK_MODEM_PROPERTY = "ro.boot.radio.allow_mock_modem";
    private static final boolean DEBUG = !"user".equals(Build.TYPE);
    /**
     * Init method to instantiate the object
     * Should only be called once.
     */
    public static PhoneConfigurationManager init(Context context,
            @NonNull FeatureFlags featureFlags) {
        synchronized (PhoneConfigurationManager.class) {
            if (sInstance == null) {
                sInstance = new PhoneConfigurationManager(context, featureFlags);
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
    private PhoneConfigurationManager(Context context, @NonNull FeatureFlags featureFlags) {
        mContext = context;
        mFeatureFlags = featureFlags;
        // TODO: send commands to modem once interface is ready.
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mRadioConfig = RadioConfig.getInstance();
        mHandler = new ConfigManagerHandler();
        mPhoneStatusMap = new HashMap<>();
        mVirtualDsdaEnabled = DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_TELEPHONY, KEY_ENABLE_VIRTUAL_DSDA, false);
        mNotifier = new DefaultPhoneNotifier(mContext, mFeatureFlags);
        DeviceConfig.addOnPropertiesChangedListener(
                DeviceConfig.NAMESPACE_TELEPHONY, Runnable::run,
                properties -> {
                    if (TextUtils.equals(DeviceConfig.NAMESPACE_TELEPHONY,
                            properties.getNamespace())) {
                        mHandler.sendEmptyMessage(EVENT_DEVICE_CONFIG_CHANGED);
                    }
                });

        notifyCapabilityChanged();

        mPhones = PhoneFactory.getPhones();

        for (Phone phone : mPhones) {
            registerForRadioState(phone);
        }
    }

    /**
     * Assign a listener to be notified of state changes.
     *
     * @param listener A listener.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener.
     *
     * @param listener A listener.
     */
    public final void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    /**
     * Updates the mapping between the slot IDs that support simultaneous calling and the
     * associated sub IDs as well as notifies listeners.
     */
    private void updateSimultaneousSubIdsFromPhoneIdMappingAndNotify() {
        if (!mFeatureFlags.simultaneousCallingIndications()) return;
        Set<Integer> slotCandidates = mSlotsSupportingSimultaneousCellularCalls.stream()
                .map(i -> mPhones[i].getSubId())
                .filter(i ->i > SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                        .collect(Collectors.toSet());
        if (mSubIdsSupportingSimultaneousCellularCalls.equals(slotCandidates))  return;
        log("updateSimultaneousSubIdsFromPhoneIdMapping update: "
                + mSubIdsSupportingSimultaneousCellularCalls + " -> " + slotCandidates);
        mSubIdsSupportingSimultaneousCellularCalls.clear();
        mSubIdsSupportingSimultaneousCellularCalls.addAll(slotCandidates);
        mNotifier.notifySimultaneousCellularCallingSubscriptionsChanged(
                mSubIdsSupportingSimultaneousCellularCalls);
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
     * Listener for listening to events in the {@link android.telephony.TelephonyRegistryManager}
     */
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    updateSimultaneousSubIdsFromPhoneIdMappingAndNotify();
                }
            };

    /**
     * If virtual DSDA is enabled for this UE, then increase maxActiveVoiceSubscriptions to 2.
     */
    private PhoneCapability maybeOverrideMaxActiveVoiceSubscriptions(
            final PhoneCapability staticCapability) {
        boolean isVDsdaEnabled = staticCapability.getLogicalModemList().size() > 1
                && mVirtualDsdaEnabled;
        boolean isBkwdCompatDsdaEnabled = mFeatureFlags.simultaneousCallingIndications()
                && mMi.getMultiSimProperty().orElse(SSSS).equals(DSDA);
        if (isVDsdaEnabled || isBkwdCompatDsdaEnabled) {
            // Since we already initialized maxActiveVoiceSubscriptions to the count the
            // modem is capable of, we are only able to increase that count via this method. We do
            // not allow a decrease of maxActiveVoiceSubscriptions:
            int updatedMaxActiveVoiceSubscriptions =
                    Math.max(staticCapability.getMaxActiveVoiceSubscriptions(), 2);
            return new PhoneCapability.Builder(staticCapability)
                    .setMaxActiveVoiceSubscriptions(updatedMaxActiveVoiceSubscriptions)
                    .build();
        } else {
            return staticCapability;
        }
    }

    private void maybeEnableCellularDSDASupport() {
        boolean bkwdsCompatDsda = mFeatureFlags.simultaneousCallingIndications()
                && getPhoneCount() > 1
                && mMi.getMultiSimProperty().orElse(SSSS).equals(DSDA);
        boolean halSupportSimulCalling = mRadioConfig != null
                && mRadioConfig.getRadioConfigProxy(null).getVersion().greaterOrEqual(
                        RIL.RADIO_HAL_VERSION_2_2)
                && getPhoneCount() > 1
                && getCellularStaticPhoneCapability().getMaxActiveVoiceSubscriptions() > 1;
        // Register for simultaneous calling support changes in the modem if the HAL supports it
        if (halSupportSimulCalling) {
            updateSimultaneousCallingSupport();
            mRadioConfig.registerForSimultaneousCallingSupportStatusChanged(mHandler,
                    EVENT_SIMULTANEOUS_CALLING_SUPPORT_CHANGED, null);
        } else if (bkwdsCompatDsda) {
            // For older devices that only declare that they support DSDA via modem config,
            // set DSDA as capable now statically.
            log("DSDA modem config detected - setting DSDA enabled");
            for (Phone p : mPhones) {
                mSlotsSupportingSimultaneousCellularCalls.add(p.getPhoneId());
            }
            updateSimultaneousSubIdsFromPhoneIdMappingAndNotify();
            notifySimultaneousCellularCallingSlotsChanged();
        }
        // Register for subId updates to notify listeners when simultaneous calling is configured
        if (mFeatureFlags.simultaneousCallingIndications()
                && (bkwdsCompatDsda || halSupportSimulCalling)) {
            Log.d(LOG_TAG, "maybeEnableCellularDSDASupport: registering "
                            + "mSubscriptionsChangedListener");
            mContext.getSystemService(TelephonyRegistryManager.class)
                    .addOnSubscriptionsChangedListener(
                            mSubscriptionsChangedListener, mHandler::post);
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
                    updateRadioCapability();
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
                        setStaticPhoneCapability((PhoneCapability) ar.result);
                        notifyCapabilityChanged();
                        for (Listener l : mListeners) {
                            l.onPhoneCapabilityChanged();
                        }
                        maybeEnableCellularDSDASupport();
                    } else {
                        log(msg.what + " failure. Not getting phone capability." + ar.exception);
                    }
                    break;
                case EVENT_DEVICE_CONFIG_CHANGED:
                    boolean isVirtualDsdaEnabled = DeviceConfig.getBoolean(
                            DeviceConfig.NAMESPACE_TELEPHONY, KEY_ENABLE_VIRTUAL_DSDA, false);
                    if (isVirtualDsdaEnabled != mVirtualDsdaEnabled) {
                        log("EVENT_DEVICE_CONFIG_CHANGED: from " + mVirtualDsdaEnabled + " to "
                                + isVirtualDsdaEnabled);
                        mVirtualDsdaEnabled = isVirtualDsdaEnabled;
                        for (Listener l : mListeners) {
                            l.onDeviceConfigChanged();
                        }
                    }
                    break;
                case EVENT_SIMULTANEOUS_CALLING_SUPPORT_CHANGED:
                case EVENT_GET_SIMULTANEOUS_CALLING_SUPPORT_DONE:
                    log("Received EVENT_SLOTS_SUPPORTING_SIMULTANEOUS_CALL_CHANGED/DONE");
                    if (getPhoneCount() < 2) {
                        if (!mSlotsSupportingSimultaneousCellularCalls.isEmpty()) {
                            mSlotsSupportingSimultaneousCellularCalls.clear();
                        }
                        break;
                    }
                    ar = (AsyncResult) msg.obj;
                    if (ar != null && ar.exception == null) {
                        List<Integer> returnedArrayList = (List<Integer>) ar.result;
                        if (!mSlotsSupportingSimultaneousCellularCalls.isEmpty()) {
                            mSlotsSupportingSimultaneousCellularCalls.clear();
                        }
                        int maxValidPhoneSlot = getPhoneCount() - 1;
                        for (int i : returnedArrayList) {
                            if (i < 0 || i > maxValidPhoneSlot) {
                                loge("Invalid slot supporting DSDA =" + i + ". Disabling DSDA.");
                                mSlotsSupportingSimultaneousCellularCalls.clear();
                                break;
                            }
                            mSlotsSupportingSimultaneousCellularCalls.add(i);
                        }
                        // Ensure the number of slots supporting cellular DSDA is valid:
                        if (mSlotsSupportingSimultaneousCellularCalls.size() > getPhoneCount() ||
                                mSlotsSupportingSimultaneousCellularCalls.size() < 2) {
                            loge("Invalid size of DSDA slots. Disabling cellular DSDA. Size of "
                                    + "mSlotsSupportingSimultaneousCellularCalls=" +
                                    mSlotsSupportingSimultaneousCellularCalls.size());
                            mSlotsSupportingSimultaneousCellularCalls.clear();
                        }
                    } else {
                        log(msg.what + " failure. Not getting logical slots that support "
                                + "simultaneous calling." + ar.exception);
                        mSlotsSupportingSimultaneousCellularCalls.clear();
                    }
                    if (mFeatureFlags.simultaneousCallingIndications()) {
                        updateSimultaneousSubIdsFromPhoneIdMappingAndNotify();
                        notifySimultaneousCellularCallingSlotsChanged();
                    }
                    break;
                default:
                    log("Unknown event: " + msg.what);
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
     * @return The updated list of logical slots that support simultaneous cellular calling from the
     * modem based on current network conditions.
     */
    public Set<Integer> getSlotsSupportingSimultaneousCellularCalls() {
        return mSlotsSupportingSimultaneousCellularCalls;
    }

    /**
     * Get the current the list of logical slots supporting simultaneous cellular calling from the
     * modem based on current network conditions.
     */
    @VisibleForTesting
    public void updateSimultaneousCallingSupport() {
        log("updateSimultaneousCallingSupport: sending the request for "
                + "getting the list of logical slots supporting simultaneous cellular calling");
        Message callback = Message.obtain(
                mHandler, EVENT_GET_SIMULTANEOUS_CALLING_SUPPORT_DONE);
        mRadioConfig.updateSimultaneousCallingSupport(callback);
        log("updateSimultaneousCallingSupport: "
                + "mSlotsSupportingSimultaneousCellularCalls = " +
                mSlotsSupportingSimultaneousCellularCalls);
    }

    /**
     * @return static overall phone capabilities for all phones, including voice overrides.
     */
    public synchronized PhoneCapability getStaticPhoneCapability() {
        boolean isDefault = mStaticCapability == null;
        PhoneCapability caps = isDefault ? getDefaultCapability() : mStaticCapability;
        caps = maybeOverrideMaxActiveVoiceSubscriptions(caps);
        log("getStaticPhoneCapability: isDefault=" + isDefault + ", caps=" + caps);
        return caps;
    }

    /**
     * @return untouched capabilities returned from the modem
     */
    private synchronized PhoneCapability getCellularStaticPhoneCapability() {
        log("getCellularStaticPhoneCapability: mStaticCapability " + mStaticCapability);
        return mStaticCapability;
    }

    /**
     * Caches the static PhoneCapability returned by the modem
     */
    public synchronized void setStaticPhoneCapability(PhoneCapability capability) {
        log("setStaticPhoneCapability: mStaticCapability " + capability);
        mStaticCapability = capability;
    }

    /**
     * Query the modem to return its static PhoneCapability and cache it
     */
    @VisibleForTesting
    public void updateRadioCapability() {
        log("updateRadioCapability: sending the request for getting PhoneCapability");
        Message callback = Message.obtain(mHandler, EVENT_GET_PHONE_CAPABILITY_DONE);
        mRadioConfig.getPhoneCapability(callback);
    }

    /**
     * get configuration related status of each phone.
     */
    public PhoneCapability getCurrentPhoneCapability() {
        return getStaticPhoneCapability();
    }

    public int getNumberOfModemsWithSimultaneousDataConnections() {
        return getStaticPhoneCapability().getMaxActiveDataSubscriptions();
    }

    public int getNumberOfModemsWithSimultaneousVoiceConnections() {
        return getStaticPhoneCapability().getMaxActiveVoiceSubscriptions();
    }

    public boolean isVirtualDsdaEnabled() {
        return mVirtualDsdaEnabled;
    }

    /**
     * Register to listen to changes in the Phone slots that support simultaneous calling.
     * @param consumer A consumer that will be used to consume the new slots supporting simultaneous
     *                 cellular calling when it changes.
     */
    public void registerForSimultaneousCellularCallingSlotsChanged(
            Consumer<Set<Integer>> consumer) {
        mSimultaneousCellularCallingListeners.add(consumer);
    }

    private void notifySimultaneousCellularCallingSlotsChanged() {
        log("notifying listeners of changes to simultaneous cellular calling - new state:"
                + mSlotsSupportingSimultaneousCellularCalls);
        for (Consumer<Set<Integer>> consumer : mSimultaneousCellularCallingListeners) {
            try {
                consumer.accept(new HashSet<>(mSlotsSupportingSimultaneousCellularCalls));
            } catch (Exception e) {
                log("Unexpected Exception encountered when notifying listener: " + e);
            }
        }
    }

    private void notifyCapabilityChanged() {
        mNotifier.notifyPhoneCapabilityChanged(getStaticPhoneCapability());
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
                SubscriptionManagerService.getInstance().markSubscriptionsInactive(phoneId);
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

            if (numOfActiveModems > 1) {
                // Check if cellular DSDA is supported. If it is, then send a request to the
                // modem to refresh the list of SIM slots that currently support DSDA based on
                // current network conditions
                maybeEnableCellularDSDASupport();
            } else {
                // The number of active modems is 0 or 1, disable cellular DSDA:
                mSlotsSupportingSimultaneousCellularCalls.clear();
                if (mFeatureFlags.simultaneousCallingIndications()) {
                    updateSimultaneousSubIdsFromPhoneIdMappingAndNotify();
                    notifySimultaneousCellularCallingSlotsChanged();
                }
            }

            // When the user enables DSDS mode, the default VOICE and SMS subId should be switched
            // to "No Preference".  Doing so will sync the network/sim settings and telephony.
            // (see b/198123192)
            if (numOfActiveModems > oldNumOfActiveModems && numOfActiveModems == 2) {
                Log.i(LOG_TAG, " onMultiSimConfigChanged: DSDS mode enabled; "
                        + "setting VOICE & SMS subId to -1 (No Preference)");

                //Set the default VOICE subId to -1 ("No Preference")
                SubscriptionManagerService.getInstance().setDefaultVoiceSubId(
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
        if (mFeatureFlags.hsumBroadcast()) {
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            mContext.sendBroadcast(intent);
        }
    }
    /**
     * This is invoked from shell commands during CTS testing only.
     * @return true if the modem service is set successfully, false otherwise.
     */
    public boolean setModemService(String serviceName) {
        log("setModemService: " + serviceName);
        boolean statusRadioConfig = false;
        boolean statusRil = false;
        final boolean isAllowed = SystemProperties.getBoolean(ALLOW_MOCK_MODEM_PROPERTY, false);
        final boolean isAllowedForBoot =
                SystemProperties.getBoolean(BOOT_ALLOW_MOCK_MODEM_PROPERTY, false);

        // Check for ALLOW_MOCK_MODEM_PROPERTY and BOOT_ALLOW_MOCK_MODEM_PROPERTY on user builds
        if (isAllowed || isAllowedForBoot || DEBUG) {
            if (mRadioConfig != null) {
                statusRadioConfig = mRadioConfig.setModemService(serviceName);
            }

            if (!statusRadioConfig) {
                loge("setModemService: switching modem service for radioconfig fail");
                return false;
            }

            for (int i = 0; i < getPhoneCount(); i++) {
                if (mPhones[i] != null) {
                    statusRil = mPhones[i].mCi.setModemService(serviceName);
                }

                if (!statusRil) {
                    loge("setModemService: switch modem for radio " + i + " fail");

                    // Disconnect the switched service
                    mRadioConfig.setModemService(null);
                    for (int t = 0; t < i; t++) {
                        mPhones[t].mCi.setModemService(null);
                    }
                    return false;
                }
            }
        } else {
            loge("setModemService is not allowed");
            return false;
        }

        return true;
    }

     /**
     * This is invoked from shell commands to query during CTS testing only.
     * @return the service name of the connected service.
     */
    public String getModemService() {
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

        /**
         * Wrapper function to query the sysprop for multi_sim_config
         */
        public Optional<String> getMultiSimProperty() {
            return TelephonyProperties.multi_sim_config();
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
