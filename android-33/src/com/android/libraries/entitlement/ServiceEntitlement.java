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

package com.android.libraries.entitlement;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.libraries.entitlement.eapaka.EapAkaApi;

import com.google.common.collect.ImmutableList;

/**
 * Implemnets protocol for carrier service entitlement configuration query and operation, based on
 * GSMA TS.43 spec.
 */
public class ServiceEntitlement {
    /**
     * App ID for Voice-Over-LTE entitlement.
     */
    public static final String APP_VOLTE = "ap2003";
    /**
     * App ID for Voice-Over-WiFi entitlement.
     */
    public static final String APP_VOWIFI = "ap2004";
    /**
     * App ID for SMS-Over-IP entitlement.
     */
    public static final String APP_SMSOIP = "ap2005";
    /**
     * App ID for on device service activation (OSDA) for companion device.
     */
    public static final String APP_ODSA_COMPANION = "ap2006";
    /**
     * App ID for on device service activation (OSDA) for primary device.
     */
    public static final String APP_ODSA_PRIMARY = "ap2009";

    private final CarrierConfig carrierConfig;
    private final EapAkaApi eapAkaApi;

    /**
     * Creates an instance for service entitlement configuration query and operation for the
     * carrier.
     *
     * @param context           context of application
     * @param carrierConfig     carrier specific configs used in the queries and operations.
     * @param simSubscriptionId the subscroption ID of the carrier's SIM on device. This indicates
     *                          which SIM to retrieve IMEI/IMSI from and perform EAP-AKA
     *                          authentication with. See
     *                          {@link android.telephony.SubscriptionManager}
     *                          for how to get the subscroption ID.
     */
    public ServiceEntitlement(Context context, CarrierConfig carrierConfig, int simSubscriptionId) {
        this.carrierConfig = carrierConfig;
        this.eapAkaApi = new EapAkaApi(context, simSubscriptionId);
    }

    @VisibleForTesting
    ServiceEntitlement(CarrierConfig carrierConfig, EapAkaApi eapAkaApi) {
        this.carrierConfig = carrierConfig;
        this.eapAkaApi = eapAkaApi;
    }

    /**
     * Retrieves service entitlement configuration. For on device service activation (ODSA) of eSIM
     * for companion/primary devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Supported {@code appId}: {@link #APP_VOLTE}, {@link #APP_VOWIFI}, {@link #APP_SMSOIP}.
     *
     * <p>This method sends an HTTP GET request to entitlement server, responds to EAP-AKA
     * challenge if needed, and returns the raw configuration doc as a string. The following
     * parameters are set in the HTTP request:
     *
     * <ul>
     * <li>"app": {@code appId}
     * <li>"vers": 0, or {@code request.configurationVersion()} if it's not 0.
     * <li>"entitlement_version": "2.0", or {@code request.entitlementVersion()} if it's not empty.
     * <li>"token": not set, or {@code request.authenticationToken()} if it's not empty.
     * <li>"IMSI": if "token" is set, set to {@link android.telephony.TelephonyManager#getImei}.
     * <li>"EAP_ID": if "token" is not set, set this parameter to trigger embedded EAP-AKA
     * authentication as decribed in TS.43 section 2.6.1. Its value is derived from IMSI as per
     * GSMA spec RCC.14 section C.2.
     * <li>"terminal_id": IMEI, or {@code request.terminalId()} if it's not empty.
     * <li>"terminal_vendor": {@link android.os.Build#MANUFACTURER}, or {@code
     * request.terminalVendor()} if it's not empty.
     * <li>"terminal_model": {@link android.os.Build#MODEL}, or {@code request.terminalModel()} if
     * it's not empty.
     * <li>"terminal_sw_version": {@llink android.os.Build.VERSION#BASE_OS}, or {@code
     * request.terminalSoftwareVersion()} if it's not empty.
     * <li>"app_name": not set, or {@code request.appName()} if it's not empty.
     * <li>"app_version": not set, or {@code request.appVersion()} if it's not empty.
     * <li>"notif_token": not set, or {@code request.notificationToken()} if it's not empty.
     * <li>"notif_action": {@code request.notificationAction()} if "notif_token" is set, otherwise
     * not set.
     * </ul>
     *
     * <p>Requires permission: READ_PRIVILEGED_PHONE_STATE, or carrier privilege.
     *
     * @param appId   an app ID string defined in TS.43 section 2.2, e.g. {@link #APP_VOWIFI}.
     * @param request contains parameters that can be used in the HTTP request.
     */
    @Nullable
    public String queryEntitlementStatus(String appId, ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return eapAkaApi.queryEntitlementStatus(ImmutableList.of(appId), carrierConfig, request);
    }

    /**
     * Retrieves service entitlement configurations for multiple app IDs in one HTTP
     * request/response. For on device service activation (ODSA) of eSIM for companion/primary
     * devices, use {@link #performEsimOdsa} instead.
     *
     * <p>Same with {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)} except that
     * multiple "app" parameters will be set in the HTTP request, in the order as they appear in
     * parameter {@code appIds}.
     */
    public String queryEntitlementStatus(ImmutableList<String> appIds,
            ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        return eapAkaApi.queryEntitlementStatus(appIds, carrierConfig, request);
    }

    /**
     * Performs on device service activation (ODSA) of eSIM for companion/primary devices.
     *
     * <p>Supported {@code appId}: {@link #APP_ODSA_COMPANION}, {@link #APP_ODSA_PRIMARY}.
     *
     * <p>Similar to {@link #queryEntitlementStatus(String, ServiceEntitlementRequest)}, this
     * method sends an HTTP GET request to entitlement server, responds to EAP-AKA challenge if
     * needed, and returns the raw configuration doc as a string. Additional parameters from {@code
     * operation} are set to the HTTP request. See {@link EsimOdsaOperation} for details.
     */
    public String performEsimOdsa(
            String appId, ServiceEntitlementRequest request, EsimOdsaOperation operation)
            throws ServiceEntitlementException {
        return eapAkaApi.performEsimOdsaOperation(appId, carrierConfig, request, operation);
    }
}
