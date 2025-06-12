/*
 * Copyright (C) 2024 The Android Open Source Project
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
import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Message;
import android.telephony.AccessNetworkConstants;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyRegistryManager;
import android.util.Log;

import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.telephony.flags.FeatureFlags;
import com.android.internal.telephony.imsphone.ImsPhone;
import com.android.internal.telephony.subscription.SubscriptionManagerService;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class SimultaneousCallingTracker {
    private static SimultaneousCallingTracker sInstance = null;
    private final Context mContext;

    /**
     * A dynamic map of all voice capable {@link Phone} objects mapped to the set of {@link Phone}
     * objects each {@link Phone} has a compatible user association with. To be considered
     * compatible based on user association, both must be associated with the same
     * {@link android.os.UserHandle} or both must be unassociated.
     */
    private Map<Phone, Set<Phone>> mVoiceCapablePhoneMap = new HashMap<>();

    @VisibleForTesting
    public boolean isDeviceSimultaneousCallingCapable = false;
    public Set<Listener> mListeners = new CopyOnWriteArraySet<>();
    private final PhoneConfigurationManager mPhoneConfigurationManager;
    private final Handler mHandler;

    /**
     * A dynamic map of all the Phone IDs mapped to the set of {@link Phone} objects each
     * {@link Phone} supports simultaneous calling (DSDA) with.
     */
    private Map<Integer, Set<Phone>> mSimultaneousCallPhoneSupportMap = new HashMap<>();
    private static final String LOG_TAG = "SimultaneousCallingTracker";
    protected static final int EVENT_SUBSCRIPTION_CHANGED         = 101;
    protected static final int EVENT_PHONE_CAPABILITY_CHANGED     = 102;
    protected static final int EVENT_MULTI_SIM_CONFIG_CHANGED     = 103;
    protected static final int EVENT_DEVICE_CONFIG_CHANGED        = 104;
    protected static final int EVENT_IMS_REGISTRATION_CHANGED     = 105;

    /** Feature flags */
    @NonNull
    private final FeatureFlags mFeatureFlags;

    /**
     * Init method to instantiate the object
     * Should only be called once.
     */
    public static SimultaneousCallingTracker init(Context context,
            @NonNull FeatureFlags featureFlags) {
        if (sInstance == null) {
            sInstance = new SimultaneousCallingTracker(context, featureFlags);
        } else {
            Log.wtf(LOG_TAG, "init() called multiple times!  sInstance = " + sInstance);
        }
        return sInstance;
    }

    /**
     * Constructor.
     * @param context context needed to send broadcast.
     */
    private SimultaneousCallingTracker(Context context, @NonNull FeatureFlags featureFlags) {
        mContext = context;
        mFeatureFlags = featureFlags;
        mHandler = new ConfigManagerHandler();
        mPhoneConfigurationManager = PhoneConfigurationManager.getInstance();
        mPhoneConfigurationManager.addListener(mPhoneConfigurationManagerListener);
        PhoneConfigurationManager.registerForMultiSimConfigChange(mHandler,
                EVENT_MULTI_SIM_CONFIG_CHANGED, null);
        TelephonyRegistryManager telephonyRegistryManager = (TelephonyRegistryManager)
                context.getSystemService(Context.TELEPHONY_REGISTRY_SERVICE);
        telephonyRegistryManager.addOnSubscriptionsChangedListener(
                mSubscriptionsChangedListener, new HandlerExecutor(mHandler));
    }

    /**
     * Static method to get instance.
     */
    public static SimultaneousCallingTracker getInstance() {
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
            if (!mFeatureFlags.simultaneousCallingIndications()) { return; }
            Log.v(LOG_TAG, "Received EVENT " + msg.what);
            switch (msg.what) {
                case EVENT_PHONE_CAPABILITY_CHANGED -> {
                    checkSimultaneousCallingDeviceCapability();
                }
                case EVENT_SUBSCRIPTION_CHANGED -> {
                    updatePhoneMapAndSimultaneousCallSupportMap();
                }
                case EVENT_MULTI_SIM_CONFIG_CHANGED -> {
                    int activeModemCount = (int) ((AsyncResult) msg.obj).result;
                    if (activeModemCount > 1) {
                        // SSIM --> MSIM: recalculate simultaneous calling supported combinations
                        updatePhoneMapAndSimultaneousCallSupportMap();
                    } else {
                        // MSIM --> SSIM: remove all simultaneous calling supported combinations
                        disableSimultaneousCallingSupport();
                        handleSimultaneousCallingSupportChanged();
                    }
                }
                case EVENT_DEVICE_CONFIG_CHANGED, EVENT_IMS_REGISTRATION_CHANGED -> {
                    updateSimultaneousCallSupportMap();
                }
                default -> Log.i(LOG_TAG, "Received unknown event: " + msg.what);
            }
        }
    }

    /**
     * Listener interface for events related to the {@link SimultaneousCallingTracker}.
     */
    public interface Listener {
        /**
         * Inform Telecom that the simultaneous calling subscription support map may have changed.
         *
         * @param simultaneousCallSubSupportMap Map of all voice capable subscription IDs mapped to
         *                                      a set containing the subscription IDs which that
         *                                      subscription is DSDA compatible with.
         */
        public void onSimultaneousCallingSupportChanged(Map<Integer,
                Set<Integer>> simultaneousCallSubSupportMap);
    }

    /**
     * Base listener implementation.
     */
    public abstract static class ListenerBase implements SimultaneousCallingTracker.Listener {
        @Override
        public void onSimultaneousCallingSupportChanged(Map<Integer,
                Set<Integer>> simultaneousCallSubSupportMap) {}
    }

    /**
     * Assign a listener to be notified of state changes.
     *
     * @param listener A listener.
     */
    public void addListener(Listener listener) {
        if (mFeatureFlags.simultaneousCallingIndications()) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener A listener.
     */
    public final void removeListener(Listener listener) {
        if (mFeatureFlags.simultaneousCallingIndications()) {
            mListeners.remove(listener);
        }
    }

    /**
     * Listener for listening to events in the {@link android.telephony.TelephonyRegistryManager}
     */
    private final SubscriptionManager.OnSubscriptionsChangedListener mSubscriptionsChangedListener =
            new SubscriptionManager.OnSubscriptionsChangedListener() {
                @Override
                public void onSubscriptionsChanged() {
                    if (!mHandler.hasMessages(EVENT_SUBSCRIPTION_CHANGED)) {
                        mHandler.sendEmptyMessage(EVENT_SUBSCRIPTION_CHANGED);
                    }
                }
            };

    /**
     * Listener for listening to events in the {@link PhoneConfigurationManager}.
     */
    private final PhoneConfigurationManager.Listener mPhoneConfigurationManagerListener =
            new PhoneConfigurationManager.Listener() {
                @Override
                public void onPhoneCapabilityChanged() {
                    if (!mHandler.hasMessages(EVENT_PHONE_CAPABILITY_CHANGED)) {
                        mHandler.sendEmptyMessage(EVENT_PHONE_CAPABILITY_CHANGED);
                    }
                }
                @Override
                public void onDeviceConfigChanged() {
                    if (!mHandler.hasMessages(EVENT_DEVICE_CONFIG_CHANGED)) {
                        mHandler.sendEmptyMessage(EVENT_DEVICE_CONFIG_CHANGED);
                    }
                }
            };

    private void checkSimultaneousCallingDeviceCapability() {
        if (mPhoneConfigurationManager.getNumberOfModemsWithSimultaneousVoiceConnections() > 1) {
            isDeviceSimultaneousCallingCapable = true;
            mPhoneConfigurationManager.registerForSimultaneousCellularCallingSlotsChanged(
                    this::onSimultaneousCellularCallingSlotsChanged);
        }
    }

    /**
     *
     * @param subId to get the slots supporting simultaneous calling with
     * @return the set of subId's that support simultaneous calling with the param subId
     */
    public Set<Integer> getSubIdsSupportingSimultaneousCalling(int subId) {
        if (!isDeviceSimultaneousCallingCapable) {
            Log.v(LOG_TAG, "Device is not simultaneous calling capable");
            return Collections.emptySet();
        }
        for (int phoneId : mSimultaneousCallPhoneSupportMap.keySet()) {
            if (PhoneFactory.getPhone(phoneId).getSubId() == subId) {
                Set<Integer> subIdsSupportingSimultaneousCalling = new HashSet<>();
                for (Phone phone : mSimultaneousCallPhoneSupportMap.get(phoneId)) {
                    subIdsSupportingSimultaneousCalling.add(phone.getSubId());
                }
                Log.d(LOG_TAG, "getSlotsSupportingSimultaneousCalling for subId=" + subId +
                        "; subIdsSupportingSimultaneousCalling=[" +
                        getStringFromSet(subIdsSupportingSimultaneousCalling) + "].");
                return subIdsSupportingSimultaneousCalling;
            }
        }
        Log.e(LOG_TAG, "getSlotsSupportingSimultaneousCalling: Subscription ID not found in"
                + " the map of voice capable phones.");
        return Collections.emptySet();
    }

    private void updatePhoneMapAndSimultaneousCallSupportMap() {
        if (!isDeviceSimultaneousCallingCapable) {
            Log.d(LOG_TAG, "Ignoring updatePhoneMapAndSimultaneousCallSupportMap since device "
                    + "is not DSDA capable.");
            return;
        }
        unregisterForImsRegistrationChanges(mVoiceCapablePhoneMap);
        mVoiceCapablePhoneMap = generateVoiceCapablePhoneMapBasedOnUserAssociation();
        Log.i(LOG_TAG, "updatePhoneMapAndSimultaneousCallSupportMap: mVoiceCapablePhoneMap.size = "
                + mVoiceCapablePhoneMap.size());
        registerForImsRegistrationChanges(mVoiceCapablePhoneMap);
        updateSimultaneousCallSupportMap();
    }

    private void updateSimultaneousCallSupportMap() {
        if (!isDeviceSimultaneousCallingCapable) {
            Log.d(LOG_TAG, "Ignoring updateSimultaneousCallSupportMap since device is not DSDA"
                    + "capable.");
            return;
        }
        mSimultaneousCallPhoneSupportMap =
                generateSimultaneousCallSupportMap(mVoiceCapablePhoneMap);
        handleSimultaneousCallingSupportChanged();
    }

    /**
     * The simultaneous cellular calling slots have changed.
     * @param slotIds The Set of slotIds that have simultaneous cellular calling.
     */
    private void onSimultaneousCellularCallingSlotsChanged(Set<Integer> slotIds) {
        //Cellular calling slots have changed - regenerate simultaneous calling support map:
        updateSimultaneousCallSupportMap();
    }

    private void disableSimultaneousCallingSupport() {
        if (!isDeviceSimultaneousCallingCapable) {
            Log.d(LOG_TAG, "Ignoring updateSimultaneousCallSupportMap since device is not DSDA"
                    + "capable.");
            return;
        }
        unregisterForImsRegistrationChanges(mVoiceCapablePhoneMap);

        // In Single-SIM mode, simultaneous calling is not supported at all:
        mSimultaneousCallPhoneSupportMap.clear();
        mVoiceCapablePhoneMap.clear();
    }

    /**
     * Registers a listener to receive IMS registration changes for all phones in the phoneMap.
     *
     * @param phoneMap Map of voice capable phones mapped to the set of phones each has a compatible
     *                 user association with.
     */
    private void registerForImsRegistrationChanges(Map<Phone, Set<Phone>> phoneMap) {
        for (Phone phone : phoneMap.keySet()) {
            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            if (imsPhone != null) {
                Log.v(LOG_TAG, "registerForImsRegistrationChanges: registering phoneId = " +
                        phone.getPhoneId());
                imsPhone.registerForImsRegistrationChanges(mHandler,
                        EVENT_IMS_REGISTRATION_CHANGED, null);
            } else {
                Log.v(LOG_TAG, "registerForImsRegistrationChanges: phone not recognized as "
                        + "ImsPhone: phoneId = " + phone.getPhoneId());
            }
        }
    }

    /**
     * Unregisters the listener to stop receiving IMS registration changes for all phones in the
     * phoneMap.
     *
     * @param phoneMap Map of voice capable phones mapped to the set of phones each has a compatible
     *                 user association with.
     */
    private void unregisterForImsRegistrationChanges(Map<Phone, Set<Phone>> phoneMap) {
        for (Phone phone : phoneMap.keySet()) {
            ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
            if (imsPhone != null) {
                imsPhone.unregisterForImsRegistrationChanges(mHandler);
            }
        }
    }

    /**
     * Generates mVoiceCapablePhoneMap by iterating through {@link PhoneFactory#getPhones()} and
     * checking whether each {@link Phone} corresponds to a valid and voice capable subscription.
     * Maps the voice capable phones to the other voice capable phones that have compatible user
     * associations
     */
    private Map<Phone, Set<Phone>> generateVoiceCapablePhoneMapBasedOnUserAssociation() {
        Map<Phone, Set<Phone>> voiceCapablePhoneMap = new HashMap<>(3);

        // Generate a map of phone slots that corresponds to valid and voice capable subscriptions:
        Phone[] allPhones = PhoneFactory.getPhones();
        for (Phone phone : allPhones) {
            int subId = phone.getSubId();
            SubscriptionInfo subInfo =
                    SubscriptionManagerService.getInstance().getSubscriptionInfo(subId);

            if (subId > SubscriptionManager.INVALID_SUBSCRIPTION_ID && subInfo != null
                    && subInfo.getServiceCapabilities()
                            .contains(SubscriptionManager.SERVICE_CAPABILITY_VOICE)) {
                Log.v(LOG_TAG, "generateVoiceCapablePhoneMapBasedOnUserAssociation: adding "
                        + "phoneId = " + phone.getPhoneId());
                voiceCapablePhoneMap.put(phone, new HashSet<>(3));
            }
        }

        Map<Phone, Set<Phone>> userAssociationPhoneMap = new HashMap<>(3);
        // Map the voice capable phones to the others that have compatible user associations:
        for (Phone phone1 : voiceCapablePhoneMap.keySet()) {
            Set<Phone> phone1UserAssociationCompatiblePhones = new HashSet<>(3);
            for (Phone phone2 : voiceCapablePhoneMap.keySet()) {
                if (phone1.getPhoneId() == phone2.getPhoneId()) { continue; }
                if (phonesHaveSameUserAssociation(phone1, phone2)) {
                    phone1UserAssociationCompatiblePhones.add(phone2);
                }
            }
            userAssociationPhoneMap.put(phone1, phone1UserAssociationCompatiblePhones);
        }

        return userAssociationPhoneMap;
    }

    private Map<Integer, Set<Phone>> generateSimultaneousCallSupportMap(
            Map<Phone, Set<Phone>> phoneMap) {
        Map<Integer, Set<Phone>> simultaneousCallSubSupportMap = new HashMap<>(3);

        // Initially populate simultaneousCallSubSupportMap based on the passed in phoneMap:
        for (Phone phone : phoneMap.keySet()) {
            simultaneousCallSubSupportMap.put(phone.getPhoneId(),
                    new HashSet<>(phoneMap.get(phone)));
        }

        // Remove phone combinations that don't support simultaneous calling from the support map:
        for (Phone phone : phoneMap.keySet()) {
            if (phone.isImsRegistered()) {
                if (mPhoneConfigurationManager.isVirtualDsdaEnabled() ||
                        phone.isImsServiceSimultaneousCallingSupportCapable(mContext)) {
                    // Check if the transport types of each phone support simultaneous IMS calling:
                    int phone1TransportType = getImsTransportType(phone);
                    if (phone1TransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN) {
                        // The transport type of this phone is WLAN so all combos are supported:
                        continue;
                    }
                    for (Phone phone2 : phoneMap.keySet()) {
                        if (phone.getPhoneId() == phone2.getPhoneId()) { continue; }
                        if (!phonesSupportSimultaneousCallingViaCellularOrWlan(phone, phone2)) {
                            simultaneousCallSubSupportMap.get(phone.getPhoneId()).remove(phone2);
                        }
                    }
                } else {
                    // IMS is registered, vDSDA is disabled, but IMS is not DSDA capable so
                    // clear the map for this phone:
                    simultaneousCallSubSupportMap.get(phone.getPhoneId()).clear();
                }
            } else {
                // Check if this phone supports simultaneous cellular calling with other phones:
                for (Phone phone2 : phoneMap.keySet()) {
                    if (phone.getPhoneId() == phone2.getPhoneId()) { continue; }
                    if (!phonesSupportSimultaneousCallingViaCellularOrWlan(phone, phone2)) {
                        simultaneousCallSubSupportMap.get(phone.getPhoneId()).remove(phone2);
                    }
                }
            }
        }
        Log.v(LOG_TAG, "generateSimultaneousCallSupportMap: returning "
                + "simultaneousCallSubSupportMap = " +
                getStringFromMap(simultaneousCallSubSupportMap));
        return simultaneousCallSubSupportMap;
    }

    /**
     * Determines whether the {@link Phone} instances have compatible user associations. To be
     * considered compatible based on user association, both must be associated with the same
     * {@link android.os.UserHandle} or both must be unassociated.
     */
    private boolean phonesHaveSameUserAssociation(Phone phone1, Phone phone2) {
        return Objects.equals(phone1.getUserHandle(), phone2.getUserHandle());
    }

    private boolean phonesSupportCellularSimultaneousCalling(Phone phone1, Phone phone2) {
        Set<Integer> slotsSupportingSimultaneousCellularCalls =
                mPhoneConfigurationManager.getSlotsSupportingSimultaneousCellularCalls();
        Log.v(LOG_TAG, "phonesSupportCellularSimultaneousCalling: modem returned slots = " +
                getStringFromSet(slotsSupportingSimultaneousCellularCalls));
        if (slotsSupportingSimultaneousCellularCalls.contains(phone1.getPhoneId()) &&
                slotsSupportingSimultaneousCellularCalls.contains(phone2.getPhoneId())) {
            return true;
        };
        return false;
    }

    private boolean phonesSupportSimultaneousCallingViaCellularOrWlan(Phone phone1, Phone phone2) {
        int phone2TransportType = getImsTransportType(phone2);
        return phone2TransportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN ||
                phonesSupportCellularSimultaneousCalling(phone1, phone2);
    }

    private void handleSimultaneousCallingSupportChanged() {
        try {
            Log.v(LOG_TAG, "handleSimultaneousCallingSupportChanged");
            // Convert mSimultaneousCallPhoneSupportMap to a map of each subId to a set of the
            // subIds it supports simultaneous calling with:
            Map<Integer, Set<Integer>> simultaneousCallSubscriptionIdMap = new HashMap<>();
            for (Integer phoneId : mSimultaneousCallPhoneSupportMap.keySet()) {
                Phone phone = PhoneFactory.getPhone(phoneId);
                if (phone == null) {
                    Log.wtf(LOG_TAG, "handleSimultaneousCallingSupportChanged: phoneId=" +
                            phoneId + " not found.");
                    return;
                }
                int subId = phone.getSubId();
                Set<Integer> supportedSubscriptionIds = new HashSet<>(3);
                for (Phone p : mSimultaneousCallPhoneSupportMap.get(phoneId)) {
                    supportedSubscriptionIds.add(p.getSubId());
                }
                simultaneousCallSubscriptionIdMap.put(subId, supportedSubscriptionIds);
            }

            // Notify listeners that simultaneous calling support has changed:
            for (Listener l : mListeners) {
                l.onSimultaneousCallingSupportChanged(simultaneousCallSubscriptionIdMap);
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, "handleSimultaneousCallingSupportChanged: Exception = " + e);
        }
    }

    private @AccessNetworkConstants.TransportType int getImsTransportType(Phone phone) {
        ImsPhone imsPhone = (ImsPhone) phone.getImsPhone();
        if (imsPhone != null) {
            return imsPhone.getTransportType();
        }
        Log.d(LOG_TAG, "getImsTransportType: IMS not supported for phone = "
            + phone);
        return AccessNetworkConstants.TRANSPORT_TYPE_INVALID;
    }

    private String getStringFromMap(Map<Integer, Set<Phone>> phoneMap) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Set<Phone>> entry : phoneMap.entrySet()) {
            sb.append("Phone ID=");
            sb.append(entry.getKey());
            sb.append(" - Simultaneous calling compatible phone IDs=[");
            sb.append(entry.getValue().stream().map(Phone::getPhoneId).map(String::valueOf)
                    .collect(Collectors.joining(", ")));
            sb.append("]; ");
        }
        return sb.toString();
    }

    private String getStringFromSet(Set<Integer> integerSet) {
        return integerSet.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
