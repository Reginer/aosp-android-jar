/*
 * Copyright (C) 2022 The Android Open Source Project
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

package android.adservices.ondevicepersonalization;

/**
 * Constants used internally in the OnDevicePersonalization Module and not used in public APIs.
 *
 * @hide
 */
public class Constants {
    // Status codes used within the ODP service or returned from service to manager classes.
    // These will be mapped to existing platform exceptions or subclasses of
    // OnDevicePersonalizationException in APIs.
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_INTERNAL_ERROR = 100;
    public static final int STATUS_NAME_NOT_FOUND = 101;
    public static final int STATUS_CLASS_NOT_FOUND = 102;
    public static final int STATUS_SERVICE_FAILED = 103;

    /**
     * Internal code that tracks user privacy is not eligible to run operation. DO NOT expose this
     * status externally.
     */
    public static final int STATUS_PERSONALIZATION_DISABLED = 104;

    public static final int STATUS_KEY_NOT_FOUND = 105;

    /** Internal error code that tracks failure to read ODP manifest settings. */
    public static final int STATUS_MANIFEST_PARSING_FAILED = 106;

    /** Internal error code that tracks misconfigured ODP manifest settings. */
    public static final int STATUS_MANIFEST_MISCONFIGURED = 107;

    /** Internal error code that tracks errors in loading the Isolated Service. */
    public static final int STATUS_ISOLATED_SERVICE_LOADING_FAILED = 108;

    /** Internal error code that tracks error when Isolated Service times out. */
    public static final int STATUS_ISOLATED_SERVICE_TIMEOUT = 109;

    /** Internal error code that tracks error when the FCP manifest is invalid or missing. */
    public static final int STATUS_FCP_MANIFEST_INVALID = 110;

    /** Internal code that tracks empty result returned from data storage or example store. */
    public static final int STATUS_SUCCESS_EMPTY_RESULT = 111;

    /** Internal code that tracks timeout exception when run operation. */
    public static final int STATUS_TIMEOUT = 112;

    /** Internal code that tracks remote exception when run operation. */
    public static final int STATUS_REMOTE_EXCEPTION = 113;

    /** Internal code that tracks method not found. */
    public static final int STATUS_METHOD_NOT_FOUND = 114;

    public static final int STATUS_CALLER_NOT_ALLOWED = 115;
    public static final int STATUS_NULL_ADSERVICES_COMMON_MANAGER = 116;

    // Internal code that tracks data access not included result returned from data storage.
    public static final int STATUS_PERMISSION_DENIED = 117;
    // Internal code that tracks local data read only result returned from data storage.
    public static final int STATUS_LOCAL_DATA_READ_ONLY = 118;
    // Internal code that tracks thread interrupted exception errors.
    public static final int STATUS_EXECUTION_INTERRUPTED = 119;
    // Internal code that tracks request timestamps invalid.
    public static final int STATUS_REQUEST_TIMESTAMPS_INVALID = 120;
    // Internal code that tracks request model table id invalid.
    public static final int STATUS_MODEL_TABLE_ID_INVALID = 122;
    // Internal code that tracks request model DB lookup failed.
    public static final int STATUS_MODEL_DB_LOOKUP_FAILED = 123;
    // Internal code that tracks request model lookup generic failure.
    public static final int STATUS_MODEL_LOOKUP_FAILURE = 124;
    // Internal code that tracks unsupported operation failure.
    public static final int STATUS_DATA_ACCESS_UNSUPPORTED_OP = 125;
    // Internal code that tracks generic data access failure.
    public static final int STATUS_DATA_ACCESS_FAILURE = 126;
    // Internal code that tracks local data access failure.
    public static final int STATUS_LOCAL_WRITE_DATA_ACCESS_FAILURE = 127;
    // Internal code that tracks parsing error.
    public static final int STATUS_PARSE_ERROR = 128;
    // Internal code that tracks non-empty but not enough data from data storage or example store.
    public static final int STATUS_SUCCESS_NOT_ENOUGH_DATA = 129;

    // Operations implemented by IsolatedService.
    public static final int OP_EXECUTE = 1;
    public static final int OP_DOWNLOAD = 2;
    public static final int OP_RENDER = 3;
    public static final int OP_WEB_VIEW_EVENT = 4;
    public static final int OP_TRAINING_EXAMPLE = 5;
    public static final int OP_WEB_TRIGGER = 6;

    // Keys for Bundle objects passed between processes.
    public static final String EXTRA_APP_PARAMS_SERIALIZED =
            "android.ondevicepersonalization.extra.app_params_serialized";
    public static final String EXTRA_CALLEE_METADATA =
            "android.ondevicepersonalization.extra.callee_metadata";
    public static final String EXTRA_DATA_ACCESS_SERVICE_BINDER =
            "android.ondevicepersonalization.extra.data_access_service_binder";
    public static final String EXTRA_FEDERATED_COMPUTE_SERVICE_BINDER =
            "android.ondevicepersonalization.extra.federated_computation_service_binder";
    public static final String EXTRA_MODEL_SERVICE_BINDER =
            "android.ondevicepersonalization.extra.model_service_binder";
    public static final String EXTRA_DESTINATION_URL =
            "android.ondevicepersonalization.extra.destination_url";
    public static final String EXTRA_EVENT_PARAMS =
            "android.ondevicepersonalization.extra.event_params";
    public static final String EXTRA_INPUT = "android.ondevicepersonalization.extra.input";
    public static final String EXTRA_LOOKUP_KEYS =
            "android.ondevicepersonalization.extra.lookup_keys";
    public static final String EXTRA_MEASUREMENT_WEB_TRIGGER_PARAMS =
            "android.adservices.ondevicepersonalization.measurement_web_trigger_params";
    public static final String EXTRA_MIME_TYPE = "android.ondevicepersonalization.extra.mime_type";
    public static final String EXTRA_OUTPUT_DATA =
            "android.ondevicepersonalization.extra.output_data";
    public static final String EXTRA_OUTPUT_BEST_VALUE =
            "android.ondevicepersonalization.extra.output_best_value";
    public static final String EXTRA_RESPONSE_DATA =
            "android.ondevicepersonalization.extra.response_data";
    public static final String EXTRA_RESULT = "android.ondevicepersonalization.extra.result";
    public static final String EXTRA_SURFACE_PACKAGE_TOKEN_STRING =
            "android.ondevicepersonalization.extra.surface_package_token_string";
    public static final String EXTRA_USER_DATA = "android.ondevicepersonalization.extra.user_data";
    public static final String EXTRA_VALUE = "android.ondevicepersonalization.extra.value";
    public static final String EXTRA_MODEL_INPUTS =
            "android.ondevicepersonalization.extra.model_inputs";
    public static final String EXTRA_MODEL_OUTPUTS =
            "android.ondevicepersonalization.extra.model_outputs";
    // Inference related constants,
    public static final String EXTRA_INFERENCE_INPUT =
            "android.ondevicepersonalization.extra.inference_input";
    public static final String EXTRA_MODEL_ID = "android.ondevicepersonalization.extra.model_id";

    // API Names for API metrics logging. Must match the values in
    // frameworks/proto_logging/stats/atoms/ondevicepersonalization/ondevicepersonalization_extension_atoms.proto
    public static final int API_NAME_UNKNOWN = 0;
    public static final int API_NAME_EXECUTE = 1;
    public static final int API_NAME_REQUEST_SURFACE_PACKAGE = 2;
    public static final int API_NAME_SERVICE_ON_EXECUTE = 3;
    public static final int API_NAME_SERVICE_ON_DOWNLOAD_COMPLETED = 4;
    public static final int API_NAME_SERVICE_ON_RENDER = 5;
    public static final int API_NAME_SERVICE_ON_EVENT = 6;
    public static final int API_NAME_SERVICE_ON_TRAINING_EXAMPLE = 7;
    public static final int API_NAME_SERVICE_ON_WEB_TRIGGER = 8;
    public static final int API_NAME_REMOTE_DATA_GET = 9;
    public static final int API_NAME_REMOTE_DATA_KEYSET = 10;
    public static final int API_NAME_LOCAL_DATA_GET = 11;
    public static final int API_NAME_LOCAL_DATA_KEYSET = 12;
    public static final int API_NAME_LOCAL_DATA_PUT = 13;
    public static final int API_NAME_LOCAL_DATA_REMOVE = 14;
    public static final int API_NAME_EVENT_URL_CREATE_WITH_RESPONSE = 15;
    public static final int API_NAME_EVENT_URL_CREATE_WITH_REDIRECT = 16;
    public static final int API_NAME_LOG_READER_GET_REQUESTS = 17;
    public static final int API_NAME_LOG_READER_GET_JOINED_EVENTS = 18;
    public static final int API_NAME_FEDERATED_COMPUTE_SCHEDULE = 19;
    public static final int API_NAME_MODEL_MANAGER_RUN = 20;
    public static final int API_NAME_FEDERATED_COMPUTE_CANCEL = 21;
    public static final int API_NAME_NOTIFY_MEASUREMENT_EVENT = 22;
    public static final int API_NAME_ADSERVICES_GET_COMMON_STATES = 23;
    public static final int API_NAME_IS_FEATURE_ENABLED = 24;

    // Data Access Service operations.
    public static final int DATA_ACCESS_OP_REMOTE_DATA_LOOKUP = 1;
    public static final int DATA_ACCESS_OP_REMOTE_DATA_KEYSET = 2;
    public static final int DATA_ACCESS_OP_GET_EVENT_URL = 3;
    public static final int DATA_ACCESS_OP_LOCAL_DATA_LOOKUP = 4;
    public static final int DATA_ACCESS_OP_LOCAL_DATA_KEYSET = 5;
    public static final int DATA_ACCESS_OP_LOCAL_DATA_PUT = 6;
    public static final int DATA_ACCESS_OP_LOCAL_DATA_REMOVE = 7;
    public static final int DATA_ACCESS_OP_GET_REQUESTS = 8;
    public static final int DATA_ACCESS_OP_GET_JOINED_EVENTS = 9;
    public static final int DATA_ACCESS_OP_GET_MODEL = 10;

    // Measurement event types for measurement events received from the OS.
    public static final int MEASUREMENT_EVENT_TYPE_WEB_TRIGGER = 1;

    // Task type for trace event logging. Must match the values in
    // frameworks/proto_logging/stats/atoms/ondevicepersonalization/ondevicepersonalization_extension_atoms.proto
    public static final int TASK_TYPE_UNKNOWN = 0;
    public static final int TASK_TYPE_EXECUTE = 1;
    public static final int TASK_TYPE_RENDER = 2;
    public static final int TASK_TYPE_DOWNLOAD = 3;
    public static final int TASK_TYPE_WEBVIEW = 4;
    public static final int TASK_TYPE_TRAINING = 5;
    public static final int TASK_TYPE_MAINTENANCE = 6;
    public static final int TASK_TYPE_WEB_TRIGGER = 7;

    // Event type for trace event logging. Must match the values in
    // frameworks/proto_logging/stats/atoms/ondevicepersonalization/ondevicepersonalization_extension_atoms.proto
    public static final int EVENT_TYPE_UNKNOWN = 0;
    public static final int EVENT_TYPE_WRITE_REQUEST_LOG = 1;
    public static final int EVENT_TYPE_WRITE_EVENT_LOG = 2;

    // Status for trace event logging. Must match the values in
    // frameworks/proto_logging/stats/atoms/ondevicepersonalization/ondevicepersonalization_extension_atoms.proto
    public static final int STATUS_REQUEST_LOG_DB_SUCCESS = 1;
    public static final int STATUS_EVENT_LOG_DB_SUCCESS = 2;
    public static final int STATUS_LOG_DB_FAILURE = 3;
    public static final int STATUS_LOG_EXCEPTION = 4;
    public static final int STATUS_REQUEST_LOG_IS_NULL = 5;
    public static final int STATUS_REQUEST_LOG_IS_EMPTY = 6;
    public static final int STATUS_EVENT_LOG_IS_NULL = 7;
    public static final int STATUS_EVENT_LOG_NOT_EXIST = 8;
    public static final int STATUS_EVENT_LOG_QUERY_NOT_EXIST = 9;

    private Constants() {}
}
