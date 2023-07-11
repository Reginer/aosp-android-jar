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

package com.android.libraries.entitlement.eapaka;

import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_EAP_AKA_SYNCHRONIZATION_FAILURE;
import static com.android.libraries.entitlement.ServiceEntitlementException.ERROR_MALFORMED_HTTP_RESPONSE;

import android.content.Context;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.libraries.entitlement.CarrierConfig;
import com.android.libraries.entitlement.EsimOdsaOperation;
import com.android.libraries.entitlement.ServiceEntitlementException;
import com.android.libraries.entitlement.ServiceEntitlementRequest;
import com.android.libraries.entitlement.http.HttpClient;
import com.android.libraries.entitlement.http.HttpConstants.RequestMethod;
import com.android.libraries.entitlement.http.HttpRequest;
import com.android.libraries.entitlement.http.HttpResponse;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;

import org.json.JSONException;
import org.json.JSONObject;

public class EapAkaApi {
    private static final String TAG = "ServiceEntitlement";

    public static final String EAP_CHALLENGE_RESPONSE = "eap-relay-packet";

    private static final String VERS = "vers";
    private static final String ENTITLEMENT_VERSION = "entitlement_version";
    private static final String TERMINAL_ID = "terminal_id";
    private static final String TERMINAL_VENDOR = "terminal_vendor";
    private static final String TERMINAL_MODEL = "terminal_model";
    private static final String TERMIAL_SW_VERSION = "terminal_sw_version";
    private static final String APP = "app";
    private static final String EAP_ID = "EAP_ID";
    private static final String IMSI = "IMSI";
    private static final String TOKEN = "token";
    private static final String NOTIF_ACTION = "notif_action";
    private static final String NOTIF_TOKEN = "notif_token";
    private static final String APP_VERSION = "app_version";
    private static final String APP_NAME = "app_name";

    private static final String OPERATION = "operation";
    private static final String OPERATION_TYPE = "operation_type";
    private static final String COMPANION_TERMINAL_ID = "companion_terminal_id";
    private static final String COMPANION_TERMINAL_VENDOR = "companion_terminal_vendor";
    private static final String COMPANION_TERMINAL_MODEL = "companion_terminal_model";
    private static final String COMPANION_TERMINAL_SW_VERSION = "companion_terminal_sw_version";
    private static final String COMPANION_TERMINAL_FRIENDLY_NAME =
            "companion_terminal_friendly_name";
    private static final String COMPANION_TERMINAL_SERVICE = "companion_terminal_service";
    private static final String COMPANION_TERMINAL_ICCID = "companion_terminal_iccid";
    private static final String COMPANION_TERMINAL_EID = "companion_terminal_eid";

    private static final String TERMINAL_ICCID = "terminal_iccid";
    private static final String TERMINAL_EID = "terminal_eid";

    private static final String TARGET_TERMINAL_ID = "target_terminal_id";
    private static final String TARGET_TERMINAL_ICCID = "target_terminal_iccid";
    private static final String TARGET_TERMINAL_EID = "target_terminal_eid";

    // In case of EAP-AKA synchronization failure, we try to recover for at most two times.
    private static final int FOLLOW_SYNC_FAILURE_MAX_COUNT = 2;

    private final Context mContext;
    private final int mSimSubscriptionId;
    private final HttpClient mHttpClient;

    public EapAkaApi(Context context, int simSubscriptionId) {
        this(context, simSubscriptionId, new HttpClient());
    }

    @VisibleForTesting
    EapAkaApi(Context context, int simSubscriptionId, HttpClient httpClient) {
        this.mContext = context;
        this.mSimSubscriptionId = simSubscriptionId;
        this.mHttpClient = httpClient;
    }

    /**
     * Retrieves raw entitlement configuration doc though EAP-AKA authentication.
     *
     * <p>Implementation based on GSMA TS.43-v5.0 2.6.1.
     *
     * @throws ServiceEntitlementException when getting an unexpected http response.
     */
    @Nullable
    public String queryEntitlementStatus(ImmutableList<String> appIds,
            CarrierConfig carrierConfig, ServiceEntitlementRequest request)
            throws ServiceEntitlementException {
        Uri.Builder urlBuilder = Uri.parse(carrierConfig.serverUrl()).buildUpon();
        appendParametersForServiceEntitlementRequest(urlBuilder, appIds, request);
        if (!TextUtils.isEmpty(request.authenticationToken())) {
            // Fast Re-Authentication flow with pre-existing auth token
            Log.d(TAG, "Fast Re-Authentication");
            return httpGet(
                    urlBuilder.toString(), carrierConfig, request.acceptContentType()).body();
        } else {
            // Full Authentication flow
            Log.d(TAG, "Full Authentication");
            HttpResponse challengeResponse =
                    httpGet(
                        urlBuilder.toString(),
                        carrierConfig,
                        ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_JSON);
            return respondToEapAkaChallenge(
                    carrierConfig,
                    challengeResponse,
                    FOLLOW_SYNC_FAILURE_MAX_COUNT,
                    request.acceptContentType())
                    .body();
        }
    }

    /**
     * Sends a follow-up HTTP request to the HTTP {@code response} using the same cookie, and
     * returns the follow-up HTTP response.
     *
     * <p>The {@code response} should contain a EAP-AKA challenge from server, and the
     * follow-up request could contain:
     *
     * <ul>
     *   <li>The EAP-AKA response message, and the follow-up response should contain the
     *       service entitlement configuration; or,
     *   <li>The EAP-AKA synchronization failure message, and the follow-up response should
     *       contain the new EAP-AKA challenge. Then this method calls itself to follow-up
     *       the new challenge and return a new response, if {@code followSyncFailureCount}
     *       is greater than zero. When this method call itself {@code followSyncFailureCount} is
     *       reduced by one to prevent infinite loop (unlikely in practice, but just in case).
     * </ul>
     *
     * @param response Challenge response from server which its content type is JSON
     */
    private HttpResponse respondToEapAkaChallenge(
            CarrierConfig carrierConfig,
            HttpResponse response,
            int followSyncFailureCount,
            String contentType)
            throws ServiceEntitlementException {
        String eapAkaChallenge;
        try {
            eapAkaChallenge = new JSONObject(response.body()).getString(EAP_CHALLENGE_RESPONSE);
        } catch (JSONException jsonException) {
            throw new ServiceEntitlementException(
                    ERROR_MALFORMED_HTTP_RESPONSE, "Failed to parse json object", jsonException);
        }
        EapAkaChallenge challenge = EapAkaChallenge.parseEapAkaChallenge(eapAkaChallenge);
        EapAkaResponse eapAkaResponse =
                EapAkaResponse.respondToEapAkaChallenge(mContext, mSimSubscriptionId, challenge);
        // This could be a successful authentication, or synchronization failure.
        if (eapAkaResponse.response() != null) { // successful authentication
            return challengeResponse(
                            eapAkaResponse.response(),
                            carrierConfig,
                            response.cookies(),
                            contentType);
        } else if (eapAkaResponse.synchronizationFailureResponse() != null) {
            Log.d(TAG, "synchronization failure");
            HttpResponse newChallenge =
                    challengeResponse(
                            eapAkaResponse.synchronizationFailureResponse(),
                            carrierConfig,
                            response.cookies(),
                            ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_JSON);
            if (followSyncFailureCount > 0) {
                return respondToEapAkaChallenge(
                        carrierConfig, newChallenge, followSyncFailureCount - 1, contentType);
            } else {
                throw new ServiceEntitlementException(
                        ERROR_EAP_AKA_SYNCHRONIZATION_FAILURE,
                        "Unable to recover from EAP-AKA synchroinization failure");
            }
        } else { // not possible
            throw new AssertionError("EapAkaResponse invalid.");
        }
    }

    private HttpResponse challengeResponse(
            String eapAkaChallengeResponse,
            CarrierConfig carrierConfig,
            ImmutableList<String> cookies,
            String contentType)
            throws ServiceEntitlementException {
        Log.d(TAG, "challengeResponse");
        JSONObject postData = new JSONObject();
        try {
            postData.put(EAP_CHALLENGE_RESPONSE, eapAkaChallengeResponse);
        } catch (JSONException jsonException) {
            throw new ServiceEntitlementException(
                    ERROR_MALFORMED_HTTP_RESPONSE, "Failed to put post data", jsonException);
        }
        HttpRequest request =
                HttpRequest.builder()
                        .setUrl(carrierConfig.serverUrl())
                        .setRequestMethod(RequestMethod.POST)
                        .setPostData(postData)
                        .addRequestProperty(HttpHeaders.ACCEPT, contentType)
                        .addRequestProperty(
                                HttpHeaders.CONTENT_TYPE,
                                ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_JSON)
                        .addRequestProperty(HttpHeaders.COOKIE, cookies)
                        .setTimeoutInSec(carrierConfig.timeoutInSec())
                        .setNetwork(carrierConfig.network())
                        .build();
        return mHttpClient.request(request);
    }

    /**
     * Retrieves raw doc of performing ODSA operations. For operation type, see {@link
     * EsimOdsaOperation}.
     *
     * <p>Implementation based on GSMA TS.43-v5.0 6.1.
     */
    public String performEsimOdsaOperation(String appId, CarrierConfig carrierConfig,
            ServiceEntitlementRequest request, EsimOdsaOperation odsaOperation)
            throws ServiceEntitlementException {
        Uri.Builder urlBuilder = Uri.parse(carrierConfig.serverUrl()).buildUpon();
        appendParametersForServiceEntitlementRequest(urlBuilder, ImmutableList.of(appId), request);
        appendParametersForEsimOdsaOperation(urlBuilder, odsaOperation);

        if (!TextUtils.isEmpty(request.authenticationToken())) {
            // Fast Re-Authentication flow with pre-existing auth token
            Log.d(TAG, "Fast Re-Authentication");
            return httpGet(
                    urlBuilder.toString(), carrierConfig, request.acceptContentType()).body();
        } else {
            // Full Authentication flow
            Log.d(TAG, "Full Authentication");
            HttpResponse challengeResponse =
                    httpGet(
                            urlBuilder.toString(),
                            carrierConfig,
                            ServiceEntitlementRequest.ACCEPT_CONTENT_TYPE_JSON);
            return respondToEapAkaChallenge(
                    carrierConfig,
                    challengeResponse,
                    FOLLOW_SYNC_FAILURE_MAX_COUNT,
                    request.acceptContentType())
                    .body();
        }
    }

    private void appendParametersForServiceEntitlementRequest(
            Uri.Builder urlBuilder, ImmutableList<String> appIds,
            ServiceEntitlementRequest request) {
        TelephonyManager telephonyManager = mContext.getSystemService(
                TelephonyManager.class).createForSubscriptionId(mSimSubscriptionId);
        if (TextUtils.isEmpty(request.authenticationToken())) {
            // EAP_ID required for initial AuthN
            urlBuilder.appendQueryParameter(
                    EAP_ID,
                    getImsiEap(telephonyManager.getSimOperator(),
                            telephonyManager.getSubscriberId()));
        } else {
            // IMSI and token required for fast AuthN.
            urlBuilder
                    .appendQueryParameter(IMSI, telephonyManager.getSubscriberId())
                    .appendQueryParameter(TOKEN, request.authenticationToken());
        }

        if (!TextUtils.isEmpty(request.notificationToken())) {
            urlBuilder
                    .appendQueryParameter(NOTIF_ACTION,
                            Integer.toString(request.notificationAction()))
                    .appendQueryParameter(NOTIF_TOKEN, request.notificationToken());
        }

        // Assign terminal ID with device IMEI if not set.
        if (TextUtils.isEmpty(request.terminalId())) {
            urlBuilder.appendQueryParameter(TERMINAL_ID, telephonyManager.getImei());
        } else {
            urlBuilder.appendQueryParameter(TERMINAL_ID, request.terminalId());
        }

        // Optional query parameters, append them if not empty
        appendOptionalQueryParameter(urlBuilder, APP_VERSION, request.appVersion());
        appendOptionalQueryParameter(urlBuilder, APP_NAME, request.appName());

        for (String appId : appIds) {
            urlBuilder.appendQueryParameter(APP, appId);
        }

        urlBuilder
                // Identity and Authentication parameters
                .appendQueryParameter(TERMINAL_VENDOR, request.terminalVendor())
                .appendQueryParameter(TERMINAL_MODEL, request.terminalModel())
                .appendQueryParameter(TERMIAL_SW_VERSION, request.terminalSoftwareVersion())
                // General Service parameters
                .appendQueryParameter(VERS, Integer.toString(request.configurationVersion()))
                .appendQueryParameter(ENTITLEMENT_VERSION, request.entitlementVersion());
    }

    private void appendParametersForEsimOdsaOperation(
            Uri.Builder urlBuilder, EsimOdsaOperation odsaOperation) {
        urlBuilder.appendQueryParameter(OPERATION, odsaOperation.operation());
        if (odsaOperation.operationType() != EsimOdsaOperation.OPERATION_TYPE_NOT_SET) {
            urlBuilder.appendQueryParameter(OPERATION_TYPE,
                    Integer.toString(odsaOperation.operationType()));
        }
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_ID,
                odsaOperation.companionTerminalId());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_VENDOR,
                odsaOperation.companionTerminalVendor());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_MODEL,
                odsaOperation.companionTerminalModel());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_SW_VERSION,
                odsaOperation.companionTerminalSoftwareVersion());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_FRIENDLY_NAME,
                odsaOperation.companionTerminalFriendlyName());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_SERVICE,
                odsaOperation.companionTerminalService());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_ICCID,
                odsaOperation.companionTerminalIccid());
        appendOptionalQueryParameter(urlBuilder, COMPANION_TERMINAL_EID,
                odsaOperation.companionTerminalEid());
        appendOptionalQueryParameter(urlBuilder, TERMINAL_ICCID,
                odsaOperation.terminalIccid());
        appendOptionalQueryParameter(urlBuilder, TERMINAL_EID, odsaOperation.terminalEid());
        appendOptionalQueryParameter(urlBuilder, TARGET_TERMINAL_ID,
                odsaOperation.targetTerminalId());
        appendOptionalQueryParameter(urlBuilder, TARGET_TERMINAL_ICCID,
                odsaOperation.targetTerminalIccid());
        appendOptionalQueryParameter(urlBuilder, TARGET_TERMINAL_EID,
                odsaOperation.targetTerminalEid());
    }

    private HttpResponse httpGet(String url, CarrierConfig carrierConfig, String contentType)
            throws ServiceEntitlementException {
        HttpRequest httpRequest =
                HttpRequest.builder()
                        .setUrl(url)
                        .setRequestMethod(RequestMethod.GET)
                        .addRequestProperty(HttpHeaders.ACCEPT, contentType)
                        .setTimeoutInSec(carrierConfig.timeoutInSec())
                        .setNetwork(carrierConfig.network())
                        .build();
        return mHttpClient.request(httpRequest);
    }

    private void appendOptionalQueryParameter(Uri.Builder urlBuilder, String key, String value) {
        if (!TextUtils.isEmpty(value)) {
            urlBuilder.appendQueryParameter(key, value);
        }
    }

    /**
     * Returns the IMSI EAP value. The resulting realm part of the Root NAI in 3GPP TS 23.003 clause
     * 19.3.2 will be in the form:
     *
     * <p>{@code 0<IMSI>@nai.epc.mnc<MNC>.mcc<MCC>.3gppnetwork.org}
     */
    @Nullable
    public static String getImsiEap(@Nullable String mccmnc, @Nullable String imsi) {
        if (mccmnc == null || mccmnc.length() < 5 || imsi == null) {
            return null;
        }

        String mcc = mccmnc.substring(0, 3);
        String mnc = mccmnc.substring(3);
        if (mnc.length() == 2) {
            mnc = "0" + mnc;
        }
        return "0" + imsi + "@nai.epc.mnc" + mnc + ".mcc" + mcc + ".3gppnetwork.org";
    }
}
