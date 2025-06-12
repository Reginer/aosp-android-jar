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

package com.android.internal.telephony.satellite;

import android.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class SatelliteConstants {
    public static final int CONFIG_DATA_SOURCE_UNKNOWN = 0;
    public static final int CONFIG_DATA_SOURCE_ENTITLEMENT = 1;
    public static final int CONFIG_DATA_SOURCE_CONFIG_UPDATER = 2;
    public static final int CONFIG_DATA_SOURCE_CARRIER_CONFIG = 3;
    public static final int CONFIG_DATA_SOURCE_DEVICE_CONFIG = 4;

    @IntDef(prefix = {"CONFIG_DATA_SOURCE_"}, value = {
            CONFIG_DATA_SOURCE_UNKNOWN,
            CONFIG_DATA_SOURCE_ENTITLEMENT,
            CONFIG_DATA_SOURCE_CONFIG_UPDATER,
            CONFIG_DATA_SOURCE_CARRIER_CONFIG,
            CONFIG_DATA_SOURCE_DEVICE_CONFIG
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigDataSource {}

    public static final int SATELLITE_ENTITLEMENT_STATUS_UNKNOWN = 0;
    public static final int SATELLITE_ENTITLEMENT_STATUS_DISABLED = 1;
    public static final int SATELLITE_ENTITLEMENT_STATUS_ENABLED = 2;
    public static final int SATELLITE_ENTITLEMENT_STATUS_INCOMPATIBLE = 3;
    public static final int SATELLITE_ENTITLEMENT_STATUS_PROVISIONING = 4;

    @IntDef(prefix = {"SATELLITE_ENTITLEMENT_STATUS_"}, value = {
            SATELLITE_ENTITLEMENT_STATUS_UNKNOWN,
            SATELLITE_ENTITLEMENT_STATUS_DISABLED,
            SATELLITE_ENTITLEMENT_STATUS_ENABLED,
            SATELLITE_ENTITLEMENT_STATUS_INCOMPATIBLE,
            SATELLITE_ENTITLEMENT_STATUS_PROVISIONING
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface SatelliteEntitlementStatus {}

    public static final int CONFIG_UPDATE_RESULT_UNKNOWN = 0;
    public static final int CONFIG_UPDATE_RESULT_SUCCESS = 1;
    public static final int CONFIG_UPDATE_RESULT_INVALID_DOMAIN = 2;
    public static final int CONFIG_UPDATE_RESULT_INVALID_VERSION = 3;
    public static final int CONFIG_UPDATE_RESULT_NO_DATA = 4;
    public static final int CONFIG_UPDATE_RESULT_NO_SATELLITE_DATA = 5;
    public static final int CONFIG_UPDATE_RESULT_PARSE_ERROR = 6;
    public static final int CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_PLMN = 7;
    public static final int CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_SUPPORTED_SERVICES = 8;
    public static final int CONFIG_UPDATE_RESULT_DEVICE_DATA_INVALID_COUNTRY_CODE = 9;
    public static final int CONFIG_UPDATE_RESULT_DEVICE_DATA_INVALID_S2_CELL_FILE = 10;
    public static final int CONFIG_UPDATE_RESULT_IO_ERROR = 11;
    public static final int CONFIG_UPDATE_RESULT_INVALID_SATELLITE_ACCESS_CONFIG_FILE = 12;

    @IntDef(prefix = {"CONFIG_UPDATE_RESULT_"}, value = {
            CONFIG_UPDATE_RESULT_UNKNOWN,
            CONFIG_UPDATE_RESULT_SUCCESS,
            CONFIG_UPDATE_RESULT_INVALID_DOMAIN,
            CONFIG_UPDATE_RESULT_INVALID_VERSION,
            CONFIG_UPDATE_RESULT_NO_DATA,
            CONFIG_UPDATE_RESULT_NO_SATELLITE_DATA,
            CONFIG_UPDATE_RESULT_PARSE_ERROR,
            CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_PLMN,
            CONFIG_UPDATE_RESULT_CARRIER_DATA_INVALID_SUPPORTED_SERVICES,
            CONFIG_UPDATE_RESULT_DEVICE_DATA_INVALID_COUNTRY_CODE,
            CONFIG_UPDATE_RESULT_DEVICE_DATA_INVALID_S2_CELL_FILE,
            CONFIG_UPDATE_RESULT_IO_ERROR,
            CONFIG_UPDATE_RESULT_INVALID_SATELLITE_ACCESS_CONFIG_FILE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigUpdateResult {}

    // Access control type is unknown
    public static final int ACCESS_CONTROL_TYPE_UNKNOWN = 0;
    // Network country code is used for satellite access decision
    public static final int ACCESS_CONTROL_TYPE_NETWORK_COUNTRY_CODE = 1;
    // Device's current location is used for satellite access decision
    public static final int ACCESS_CONTROL_TYPE_CURRENT_LOCATION = 2;
    // Device's last known location is used for satellite access decision
    public static final int ACCESS_CONTROL_TYPE_LAST_KNOWN_LOCATION = 3;
    // Cached country codes are used for satellite access decision
    public static final int ACCESS_CONTROL_TYPE_CACHED_COUNTRY_CODE = 4;

    @IntDef(prefix = {"ACCESS_CONTROL_TYPE_"}, value = {
            ACCESS_CONTROL_TYPE_UNKNOWN,
            ACCESS_CONTROL_TYPE_NETWORK_COUNTRY_CODE,
            ACCESS_CONTROL_TYPE_CURRENT_LOCATION,
            ACCESS_CONTROL_TYPE_LAST_KNOWN_LOCATION,
            ACCESS_CONTROL_TYPE_CACHED_COUNTRY_CODE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessControlType {}

    //// Unknown reason.
    public static final int TRIGGERING_EVENT_UNKNOWN = 0;
    // Satellite Access Controller has been triggered by an external event.
    public static final int TRIGGERING_EVENT_EXTERNAL_REQUEST = 1;
    // Satellite Access Controller has been triggered by an MCC change event.
    public static final int TRIGGERING_EVENT_MCC_CHANGED = 2;
    //Satellite Access Controller has been triggered due to the location setting being enabled.
    public static final int TRIGGERING_EVENT_LOCATION_SETTINGS_ENABLED = 3;

    @IntDef(prefix = {"TRIGGERING_EVENT_"}, value = {
            TRIGGERING_EVENT_UNKNOWN,
            TRIGGERING_EVENT_EXTERNAL_REQUEST,
            TRIGGERING_EVENT_MCC_CHANGED,
            TRIGGERING_EVENT_LOCATION_SETTINGS_ENABLED
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TriggeringEvent {}
}
