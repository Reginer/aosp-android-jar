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
    public static final int STATUS_PERSONALIZATION_DISABLED = 104;
    public static final int STATUS_KEY_NOT_FOUND = 105;

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
    public static final int API_NAME_FEDERATED_COMPUTE_CANCEL = 21;
    public static final int API_NAME_MODEL_MANAGER_RUN = 20;

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

    private Constants() {}
}
