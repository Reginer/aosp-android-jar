/*
 * Copyright (c) 2015, Motorola Mobility LLC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     - Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     - Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     - Neither the name of Motorola Mobility nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL MOTOROLA MOBILITY LLC BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package com.android.service.ims;

import android.content.Context;
import android.os.PersistableBundle;
import android.telephony.AccessNetworkConstants;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.ims.ImsException;
import android.telephony.ims.ImsManager;
import android.telephony.ims.ImsMmTelManager;
import android.telephony.ims.ImsRcsManager;
import android.telephony.ims.ProvisioningManager;
import android.telephony.ims.feature.MmTelFeature;
import android.telephony.ims.stub.ImsRegistrationImplBase;

import com.android.ims.internal.Logger;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RcsSettingUtils {
    static private Logger logger = Logger.getLogger("RcsSettingUtils");
    private static final int TIMEOUT_GET_CONFIGURATION_MS = 5000;

    // Default number of entries for getMaxNumbersInRCL
    private static final int DEFAULT_NUM_ENTRIES_IN_RCL = 100;
    // Default for getCapabPollListSubExp in seconds.
    private static final int DEFAULT_CAPABILITY_POLL_LIST_SUB_EXPIRATION_SEC = 30;
    // Default for getAvailabilityCacheExpiration in seconds.
    private static final int DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC = 30;
    // Default for getPublishThrottle in milliseconds
    private static final int DEFAULT_PUBLISH_THROTTLE_MS = 60000;

    public static boolean isVoLteProvisioned(int subId) {
        try {
            boolean isProvisioned;
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            isProvisioned = manager.getProvisioningStatusForCapability(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
            logger.debug("isVoLteProvisioned=" + isProvisioned);
            return isProvisioned;
        } catch (Exception e) {
            logger.debug("isVoLteProvisioned, exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isVowifiProvisioned(int subId) {
        try {
            boolean isProvisioned;
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            isProvisioned = manager.getProvisioningStatusForCapability(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_IWLAN);
            logger.debug("isVowifiProvisioned=" + isProvisioned);
            return isProvisioned;
        } catch (Exception e) {
            logger.debug("isVowifiProvisioned, exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isLvcProvisioned(int subId) {
        try {
            boolean isProvisioned;
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            isProvisioned = manager.getProvisioningStatusForCapability(
                    MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
            logger.debug("isLvcProvisioned=" + isProvisioned);
            return isProvisioned;
        } catch (Exception e) {
            logger.debug("isLvcProvisioned, exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isEabProvisioned(Context context, int subId) {
        boolean isProvisioned = false;
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            logger.debug("isEabProvisioned: no valid subscriptions!");
            return false;
        }
        CarrierConfigManager configManager = (CarrierConfigManager)
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle config = configManager.getConfigForSubId(subId);
            if (config != null && !config.getBoolean(
                    CarrierConfigManager.KEY_CARRIER_VOLTE_PROVISIONED_BOOL)) {
                // If we don't need provisioning, just return true.
                return true;
            }
        }
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            isProvisioned = manager.getRcsProvisioningStatusForCapability(
                    ImsRcsManager.CAPABILITY_TYPE_PRESENCE_UCE,
                    ImsRegistrationImplBase.REGISTRATION_TECH_LTE);
        } catch (Exception e) {
            logger.debug("isEabProvisioned: exception=" + e.getMessage());
        }
        logger.debug("isEabProvisioned=" + isProvisioned);
        return isProvisioned;
    }

    public static boolean isPublishEnabled(Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            logger.debug("isPublishEnabled: no valid subscriptions!");
            return false;
        }
        CarrierConfigManager configManager = (CarrierConfigManager)
                context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            PersistableBundle config = configManager.getConfigForSubId(subId);
            return (config != null) && config.getBoolean(
                    CarrierConfigManager.Ims.KEY_ENABLE_PRESENCE_PUBLISH_BOOL, false);
        }
        return false;
    }

    public static boolean hasUserEnabledContactDiscovery(Context context, int subId) {
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            logger.debug("hasUserEnabledContactDiscovery: no valid subscriptions!");
            return false;
        }
        try {
            ImsManager imsManager = context.getSystemService(ImsManager.class);
            ImsRcsManager rcsManager = imsManager.getImsRcsManager(subId);
            return rcsManager.getUceAdapter().isUceSettingEnabled();
        } catch (Exception e) {
            logger.warn("hasUserEnabledContactDiscovery: Exception = " + e.getMessage());
            return false;
        }
    }

    public static int getSIPT1Timer(int subId) {
        int sipT1Timer = 0;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            sipT1Timer = manager.getProvisioningIntValue(ProvisioningManager.KEY_T1_TIMER_VALUE_MS);
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("getSIPT1Timer: exception=" + e.getMessage());
        }
        logger.debug("getSIPT1Timer=" + sipT1Timer);
        return sipT1Timer;
    }

    /**
     * Capability discovery status of Enabled (1), or Disabled (0).
     */
    public static boolean getCapabilityDiscoveryEnabled(int subId) {
        boolean capabilityDiscoveryEnabled = false;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            capabilityDiscoveryEnabled = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_CAPABILITY_DISCOVERY_ENABLED) ==
                    ProvisioningManager.PROVISIONING_VALUE_ENABLED;
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("capabilityDiscoveryEnabled: exception=" + e.getMessage());
        }
        logger.debug("capabilityDiscoveryEnabled=" + capabilityDiscoveryEnabled);
        return capabilityDiscoveryEnabled;
    }

    /**
     * The Maximum number of MDNs contained in one Request Contained List.
     */
    public static int getMaxNumbersInRCL(int subId) {
        int maxNumbersInRCL = DEFAULT_NUM_ENTRIES_IN_RCL;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            maxNumbersInRCL = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_MAX_NUM_ENTRIES_IN_RCL);
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("getMaxNumbersInRCL: exception=" + e.getMessage());
        }
        logger.debug("getMaxNumbersInRCL=" + maxNumbersInRCL);
        return maxNumbersInRCL;
    }

    /**
     * Expiration timer for subscription of a Request Contained List, used in capability polling.
     */
    public static int getCapabPollListSubExp(int subId) {
        int capabPollListSubExp = DEFAULT_CAPABILITY_POLL_LIST_SUB_EXPIRATION_SEC;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            capabPollListSubExp = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_CAPABILITY_POLL_LIST_SUB_EXP_SEC);
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("getCapabPollListSubExp: exception=" + e.getMessage());
        }
        logger.debug("getCapabPollListSubExp=" + capabPollListSubExp);
        return capabPollListSubExp;
    }

    /**
     * Period of time the availability information of a contact is cached on device.
     */
    public static int getAvailabilityCacheExpiration(int subId) {
        int availabilityCacheExpiration = DEFAULT_AVAILABILITY_CACHE_EXPIRATION_SEC;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            availabilityCacheExpiration = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_AVAILABILITY_CACHE_EXPIRATION_SEC);
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("getAvailabilityCacheExpiration: exception=" + e.getMessage());
        }
        logger.debug("getAvailabilityCacheExpiration=" + availabilityCacheExpiration);
        return availabilityCacheExpiration;
    }

    public static int getPublishThrottle(int subId) {
        // Default
        int publishThrottle = DEFAULT_PUBLISH_THROTTLE_MS;
        try {
            ProvisioningManager manager = ProvisioningManager.createForSubscriptionId(subId);
            publishThrottle = manager.getProvisioningIntValue(
                    ProvisioningManager.KEY_RCS_PUBLISH_SOURCE_THROTTLE_MS);
        } catch (Exception e) {
            // If there is no active subscriptions, this will throw an exception.
            logger.debug("publishThrottle: exception=" + e.getMessage());
        }
        logger.debug("publishThrottle=" + publishThrottle);
        return publishThrottle;
    }

    public static boolean isVtEnabledByUser(int subId) {
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
            return mmTelManager.isVtSettingEnabled();
        } catch (Exception e) {
            logger.warn("isVtEnabledByUser exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isWfcEnabledByUser(int subId) {
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
        return mmTelManager.isVoWiFiSettingEnabled();
        } catch (Exception e) {
            logger.warn("isWfcEnabledByUser exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isAdvancedCallingEnabledByUser(int subId) {
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
            return mmTelManager.isAdvancedCallingSettingEnabled();
        } catch (Exception e) {
            logger.warn("isAdvancedCallingEnabledByUser exception = " + e.getMessage());
            return false;
        }
    }

    public static boolean isVoLteSupported(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        LinkedBlockingQueue<Boolean> resultQueue = new LinkedBlockingQueue<>(1);
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
            mmTelManager.isSupported(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN, Runnable::run, resultQueue::offer);
        } catch (ImsException e) {
            logger.warn("isVoLteSupported: ImsException = " + e.getMessage());
            return false;
        }
        try {
            Boolean result = resultQueue.poll(TIMEOUT_GET_CONFIGURATION_MS, TimeUnit.MILLISECONDS);
            return (result != null) ? result : false;
        } catch (InterruptedException e) {
            logger.warn("isVoLteSupported, InterruptedException=" + e.getMessage());
            return false;
        }
    }

    public static boolean isVoWiFiSupported(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        LinkedBlockingQueue<Boolean> resultQueue = new LinkedBlockingQueue<>(1);
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
            mmTelManager.isSupported(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VOICE,
                    AccessNetworkConstants.TRANSPORT_TYPE_WLAN, Runnable::run, resultQueue::offer);
        } catch (ImsException e) {
            logger.warn("isVoWiFiSupported: ImsException = " + e.getMessage());
            return false;
        }
        try {
            Boolean result = resultQueue.poll(TIMEOUT_GET_CONFIGURATION_MS, TimeUnit.MILLISECONDS);
            return (result != null) ? result : false;
        } catch (InterruptedException e) {
            logger.warn("isVoWiFiSupported, InterruptedException=" + e.getMessage());
            return false;
        }
    }

    public static boolean isVtSupported(int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        LinkedBlockingQueue<Boolean> resultQueue = new LinkedBlockingQueue<>(1);
        try {
            ImsMmTelManager mmTelManager = ImsMmTelManager.createForSubscriptionId(subId);
            mmTelManager.isSupported(MmTelFeature.MmTelCapabilities.CAPABILITY_TYPE_VIDEO,
                    AccessNetworkConstants.TRANSPORT_TYPE_WWAN, Runnable::run, resultQueue::offer);
        } catch (ImsException e) {
            logger.warn("isVoWiFiSupported: ImsException = " + e.getMessage());
            return false;
        }
        try {
            Boolean result = resultQueue.poll(TIMEOUT_GET_CONFIGURATION_MS, TimeUnit.MILLISECONDS);
            return (result != null) ? result : false;
        } catch (InterruptedException e) {
            logger.warn("isVtSupported, InterruptedException=" + e.getMessage());
            return false;
        }
    }

    public static int getDefaultSubscriptionId(Context context) {
        SubscriptionManager sm = context.getSystemService(SubscriptionManager.class);
        if (sm == null) return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            List<SubscriptionInfo> infos = sm.getActiveSubscriptionInfoList();
        if (infos == null || infos.isEmpty()) {
            // There are no active subscriptions right now.
            return SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        // This code does not support MSIM unfortunately, so only provide presence on the default
        // voice subscription that the user chose.
        int defaultSub = SubscriptionManager.getDefaultVoiceSubscriptionId();
        if (!SubscriptionManager.isValidSubscriptionId(defaultSub)) {
            // The voice sub may not have been specified, in this case, use the default data.
            defaultSub = SubscriptionManager.getDefaultDataSubscriptionId();
        }
        // If the user has no default set, just pick the first as backup.
        if (!SubscriptionManager.isValidSubscriptionId(defaultSub)) {
            for (SubscriptionInfo info : infos) {
                if (!info.isOpportunistic()) {
                    defaultSub = info.getSubscriptionId();
                    break;
                }
            }
        }
        return defaultSub;
    }
}

