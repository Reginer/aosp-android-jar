/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony.analytics;

import android.provider.BaseColumns;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Defines the tables classes which are present in the Telephony Analytics Database, private
 * constructor to prevent instantiation.
 */
public final class TelephonyAnalyticsDatabase {
    private TelephonyAnalyticsDatabase() {}

    public static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());

    /**
     * CallAnalyticsTable class defines the columns in the CallTable Implements the BaseColumns
     * class.
     */
    public static final class CallAnalyticsTable implements BaseColumns {
        public static final String TABLE_NAME = "CallDataLogs";
        public static final String LOG_DATE = "LogDate";
        public static final String CALL_STATUS = "CallStatus";
        public static final String CALL_TYPE = "CallType";
        public static final String RAT = "RAT";
        public static final String SLOT_ID = "SlotID";
        public static final String FAILURE_REASON = "FailureReason";
        public static final String RELEASE_VERSION = "ReleaseVersion";
        public static final String COUNT = "Count";
    }

    /**
     * SmsMmsAnalyticsTable class defines the columns in the SmsMmsTable Implements the BaseColumns
     * class.
     */
    public static final class SmsMmsAnalyticsTable implements BaseColumns {
        public static final String TABLE_NAME = "SmsMmsDataLogs";
        public static final String LOG_DATE = "LogDate";
        public static final String SMS_MMS_STATUS = "SmsMmsStatus";
        public static final String SMS_MMS_TYPE = "SmsMmsType";
        public static final String SLOT_ID = "SlotID";
        public static final String RAT = "RAT";
        public static final String FAILURE_REASON = "FailureReason";
        public static final String RELEASE_VERSION = "ReleaseVersion";
        public static final String COUNT = "Count";
    }

    /**
     * ServiceStateAnalyticsTable class defines the columns in the ServiceStateTable Implements the
     * BaseColumns class.
     */
    public static final class ServiceStateAnalyticsTable implements BaseColumns {
        public static final String TABLE_NAME = "ServiceStateLogs";
        public static final String LOG_DATE = "LogDate";
        public static final String TIME_DURATION = "TimeDuration";
        public static final String SLOT_ID = "SlotID";
        public static final String RAT = "RAT";
        public static final String DEVICE_STATUS = "DeviceStatus";
        public static final String RELEASE_VERSION = "ReleaseVersion";
    }
}
