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
            CONFIG_UPDATE_RESULT_IO_ERROR
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConfigUpdateResult {}
}
