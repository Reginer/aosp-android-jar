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

import android.os.Build;
import android.os.Build.VERSION;

import com.google.auto.value.AutoValue;

/**
 * Service entitlement HTTP request parameters, as defiend in GSMA spec TS.43 section 2.2.
 */
@AutoValue
public abstract class ServiceEntitlementRequest {
    /** Disables notification token. */
    public static final int NOTICATION_ACTION_DISABLE = 0;
    /** Enables FCM notification token. */
    public static final int NOTICATION_ACTION_ENABLE_FCM = 2;
    /** Accepts the content type in XML format. */
    public static final String ACCEPT_CONTENT_TYPE_XML = "text/vnd.wap.connectivity-xml";
    /** Accepts the content type in JSON format. */
    public static final String ACCEPT_CONTENT_TYPE_JSON =
            "application/vnd.gsma.eap-relay.v1.0+json";
    /** Accepts the content type in JSON or XML format. */
    public static final String ACCEPT_CONTENT_TYPE_JSON_AND_XML =
            "application/vnd.gsma.eap-relay.v1.0+json, text/vnd.wap.connectivity-xml";
    /** Default value of configuration version. */
    public static final int DEFAULT_CONFIGURATION_VERSION = 0;
    /** Default value of entitlement version. */
    public static final String DEFAULT_ENTITLEMENT_VERSION = "2.0";


    /**
     * Returns the version of configuration currently stored on the client. Used by HTTP parameter
     * "vers".
     */
    public abstract int configurationVersion();

    /**
     * Returns the version of the entitlement specification. Used by HTTP parameter
     * "entitlement_version".
     */
    public abstract String entitlementVersion();

    /**
     * Returns the authentication token. Used by HTTP parameter "token".
     */
    public abstract String authenticationToken();

    /**
     * Returns the unique identifier of the device like IMEI. Used by HTTP parameter "terminal_id".
     */
    public abstract String terminalId();

    /**
     * Returns the OEM of the device. Used by HTTP parameter "terminal_vendor".
     */
    public abstract String terminalVendor();

    /**
     * Returns the model of the device. Used by HTTP parameter "terminal_model".
     */
    public abstract String terminalModel();

    /**
     * Returns the software version of the device. Used by HTTP parameter "terminal_sw_version".
     */
    public abstract String terminalSoftwareVersion();

    /**
     * Returns the name of the device application making the request. Used by HTTP parameter
     * "app_name".
     */
    public abstract String appName();

    /**
     * Returns the version of the device application making the request. Used by HTTP parameter
     * "app_version".
     */
    public abstract String appVersion();

    /**
     * Returns the FCM registration token used to register for entitlement configuration request
     * from network. Used by HTTP parameter "notif_token".
     */
    public abstract String notificationToken();

    /**
     * Returns the action associated with the FCM registration token. Used by HTTP parameter
     * "notif_action".
     *
     * @see #NOTICATION_ACTION_ENABLE_FCM
     * @see #NOTICATION_ACTION_DISABLE
     */
    public abstract int notificationAction();

    /**
     * Returns the accepted content type of http response.
     *
     * @see #ACCEPT_CONTENT_TYPE_XML
     * @see #ACCEPT_CONTENT_TYPE_JSON
     * @see #ACCEPT_CONTENT_TYPE_JSON_AND_XML
     */
    public abstract String acceptContentType();

    /**
     * Returns a new {@link Builder} object.
     */
    public static Builder builder() {
        return new AutoValue_ServiceEntitlementRequest.Builder()
                .setConfigurationVersion(DEFAULT_CONFIGURATION_VERSION)
                .setEntitlementVersion(DEFAULT_ENTITLEMENT_VERSION)
                .setAuthenticationToken("")
                .setTerminalId("")
                .setTerminalVendor(Build.MANUFACTURER)
                .setTerminalModel(Build.MODEL)
                .setTerminalSoftwareVersion(VERSION.BASE_OS)
                .setAppName("")
                .setAppVersion("")
                .setNotificationToken("")
                .setNotificationAction(NOTICATION_ACTION_ENABLE_FCM)
                .setAcceptContentType(ACCEPT_CONTENT_TYPE_JSON_AND_XML);
    }

    /**
     * Builder.
     */
    @AutoValue.Builder
    public abstract static class Builder {
        /**
         * Sets the version of configuration currently stored on the client. Used by HTTP parameter
         * "vers".
         *
         * <p>If not set, default to {@link #DEFAULT_CONFIGURATION_VERSION} indicating no existing
         * configuration.
         */
        public abstract Builder setConfigurationVersion(int value);

        /**
         * Sets the current version of the entitlement specification. Used by HTTP parameter
         * "entitlement_version".
         *
         * <p>If not set, default to {@link #DEFAULT_ENTITLEMENT_VERSION} base on TS.43-v5.0.
         */
        public abstract Builder setEntitlementVersion(String value);

        /**
         * Sets the authentication token. Used by HTTP parameter "token".
         *
         * <p>If not set, will trigger embedded EAP-AKA authentication as decribed in TS.43 section
         * 2.6.1.
         */
        public abstract Builder setAuthenticationToken(String value);

        /**
         * Sets the unique identifier of the device like IMEI. Used by HTTP parameter
         * "terminal_id".
         *
         * <p>If not set, will use the device IMEI.
         */
        public abstract Builder setTerminalId(String value);

        /**
         * Sets the OEM of the device. Used by HTTP parameter "terminal_vendor".
         *
         * <p>If not set, will use {@link android.os.Build#MANUFACTURER}.
         */
        public abstract Builder setTerminalVendor(String value);

        /**
         * Sets the model of the device. Used by HTTP parameter "terminal_model".
         *
         * <p>If not set, will use {@link android.os.Build#MODEL}.
         */
        public abstract Builder setTerminalModel(String value);

        /**
         * Sets the software version of the device. Used by HTTP parameter "terminal_sw_version".
         *
         * <p>If not set, will use {@link android.os.Build.VERSION#BASE_OS}.
         */
        public abstract Builder setTerminalSoftwareVersion(String value);

        /**
         * Sets the name of the device application making the request. Used by HTTP parameter
         * "app_name".
         *
         * <p>Optional.
         */
        public abstract Builder setAppName(String value);

        /**
         * Sets the version of the device application making the request. Used by HTTP parameter
         * "app_version".
         *
         * <p>Optional.
         */
        public abstract Builder setAppVersion(String value);

        /**
         * Sets the FCM registration token used to register for entitlement configuration request
         * from network. Used by HTTP parameter "notif_token".
         *
         * <p>Optional.
         */
        public abstract Builder setNotificationToken(String value);

        /**
         * Sets the action associated with the FCM registration token. Used by HTTP parameter
         * "notif_action".
         *
         * <p>Required if a token is set with {@link #setNotificationToken}, and default to {@link
         * #NOTICATION_ACTION_ENABLE_FCM}; otherwise ignored.
         *
         * @see #NOTICATION_ACTION_ENABLE_FCM
         * @see #NOTICATION_ACTION_DISABLE
         */
        public abstract Builder setNotificationAction(int value);

        /**
         * Sets the configuration document format the caller accepts, e.g. XML or JSON. Used by HTTP
         * request header "Accept".
         *
         * <p>If not set, will use {@link #ACCEPT_CONTENT_TYPE_JSON_AND_XML}.
         *
         * @see #ACCEPT_CONTENT_TYPE_XML
         * @see #ACCEPT_CONTENT_TYPE_JSON
         * @see #ACCEPT_CONTENT_TYPE_JSON_AND_XML
         */
        public abstract Builder setAcceptContentType(String contentType);

        public abstract ServiceEntitlementRequest build();
    }
}
