/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.ims.rcs.uce.util;

import android.telephony.ims.RcsUceAdapter;

import com.android.ims.rcs.uce.UceController;
import com.android.ims.rcs.uce.UceController.RequestType;

/**
 * Define the network sip code and the reason.
 */
public class NetworkSipCode {
    public static final int SIP_CODE_OK = 200;
    public static final int SIP_CODE_ACCEPTED = 202;
    public static final int SIP_CODE_BAD_REQUEST = 400;
    public static final int SIP_CODE_FORBIDDEN = 403;
    public static final int SIP_CODE_NOT_FOUND = 404;
    public static final int SIP_CODE_METHOD_NOT_ALLOWED = 405;
    public static final int SIP_CODE_REQUEST_TIMEOUT = 408;
    public static final int SIP_CODE_REQUEST_ENTITY_TOO_LARGE = 413;
    public static final int SIP_CODE_INTERVAL_TOO_BRIEF = 423;
    public static final int SIP_CODE_TEMPORARILY_UNAVAILABLE = 480;
    public static final int SIP_CODE_BAD_EVENT = 489;
    public static final int SIP_CODE_BUSY = 486;
    public static final int SIP_CODE_SERVER_INTERNAL_ERROR = 500;
    public static final int SIP_CODE_SERVICE_UNAVAILABLE = 503;
    public static final int SIP_CODE_SERVER_TIMEOUT = 504;
    public static final int SIP_CODE_BUSY_EVERYWHERE = 600;
    public static final int SIP_CODE_DECLINE = 603;
    public static final int SIP_CODE_DOES_NOT_EXIST_ANYWHERE = 604;

    public static final String SIP_OK = "OK";
    public static final String SIP_ACCEPTED = "Accepted";
    public static final String SIP_BAD_REQUEST = "Bad Request";
    public static final String SIP_SERVICE_UNAVAILABLE = "Service Unavailable";
    public static final String SIP_INTERNAL_SERVER_ERROR = "Internal Server Error";
    public static final String SIP_NOT_REGISTERED = "User not registered";
    public static final String SIP_NOT_AUTHORIZED_FOR_PRESENCE = "not authorized for presence";

    /**
     * Convert the given SIP CODE to the Contact uce capabilities error.
     * @param sipCode The SIP code of the request response.
     * @param reason The reason of the request response.
     * @param requestType The type of this request.
     * @return The RCS contact UCE capabilities error which is defined in RcsUceAdapter.
     */
    public static int getCapabilityErrorFromSipCode(int sipCode, String reason,
            @RequestType int requestType) {
        int uceError;
        switch (sipCode) {
            case NetworkSipCode.SIP_CODE_FORBIDDEN:   // 403
            case NetworkSipCode.SIP_CODE_SERVER_TIMEOUT:   // 504
                if(requestType == UceController.REQUEST_TYPE_PUBLISH) {
                    // Not provisioned for PUBLISH request.
                    uceError = RcsUceAdapter.ERROR_NOT_AUTHORIZED;
                } else {
                    // Check the reason for CAPABILITY request
                    if (NetworkSipCode.SIP_NOT_REGISTERED.equalsIgnoreCase(reason)) {
                        // Not registered with IMS. Device shall register to IMS.
                        uceError = RcsUceAdapter.ERROR_NOT_REGISTERED;
                    } else if (NetworkSipCode.SIP_NOT_AUTHORIZED_FOR_PRESENCE.equalsIgnoreCase(
                            reason)) {
                        // Not provisioned for EAB. Device shall not retry.
                        uceError = RcsUceAdapter.ERROR_NOT_AUTHORIZED;
                    } else {
                        // The network has responded SIP 403 error with no reason.
                        uceError = RcsUceAdapter.ERROR_FORBIDDEN;
                    }
                }
                break;
            case NetworkSipCode.SIP_CODE_NOT_FOUND:              // 404
                if(requestType == UceController.REQUEST_TYPE_PUBLISH) {
                    // Not provisioned for PUBLISH request.
                    uceError = RcsUceAdapter.ERROR_NOT_AUTHORIZED;
                } else {
                    uceError = RcsUceAdapter.ERROR_NOT_FOUND;
                }
                break;
            case NetworkSipCode.SIP_CODE_REQUEST_TIMEOUT:        // 408
                uceError = RcsUceAdapter.ERROR_REQUEST_TIMEOUT;
                break;
            case NetworkSipCode.SIP_CODE_INTERVAL_TOO_BRIEF:     // 423
                // Rejected by the network because the requested expiry interval is too short.
                uceError = RcsUceAdapter.ERROR_GENERIC_FAILURE;
                break;
            case NetworkSipCode.SIP_CODE_BAD_EVENT:
                uceError = RcsUceAdapter.ERROR_FORBIDDEN;        // 489
                break;
            case NetworkSipCode.SIP_CODE_SERVER_INTERNAL_ERROR:  // 500
            case NetworkSipCode.SIP_CODE_SERVICE_UNAVAILABLE:    // 503
                // The network is temporarily unavailable or busy.
                uceError = RcsUceAdapter.ERROR_SERVER_UNAVAILABLE;
                break;
            default:
                uceError = RcsUceAdapter.ERROR_GENERIC_FAILURE;
                break;
        }
        return uceError;
    }
}
