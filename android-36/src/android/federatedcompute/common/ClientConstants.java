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

package android.federatedcompute.common;

/**
 * Constants for FederatedCompute packages and services.
 *
 * @hide
 */
public final class ClientConstants {
    // Status code constants.
    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_INTERNAL_ERROR = 1;
    public static final int STATUS_TRAINING_FAILED = 2;
    public static final int STATUS_KILL_SWITCH_ENABLED = 3;
    public static final int STATUS_NOT_ENOUGH_DATA = 4;

    public static final String EXTRA_POPULATION_NAME = "android.federatedcompute.population_name";

    public static final String EXTRA_TASK_ID = "android.federatedcompute.task_id";

    public static final String EXTRA_CONTEXT_DATA = "android.federatedcompute.context_data";
    public static final String EXTRA_ELIGIBILITY_MIN_EXAMPLE =
            "android.federatedcompute.eligibility_min_example";

    public static final String EXTRA_COMPUTATION_RESULT =
            "android.federatedcompute.computation_result";

    public static final String EXTRA_EXAMPLE_CONSUMPTION_LIST =
            "android.federatedcompute.example_consumption_list";

    // ExampleStoreService related constants.
    public static final String EXAMPLE_STORE_ACTION = "android.federatedcompute.EXAMPLE_STORE";
    public static final String EXTRA_EXAMPLE_ITERATOR_CRITERIA =
            "android.federatedcompute.example_iterator_criteria";
    public static final String EXTRA_EXAMPLE_ITERATOR_RESUMPTION_TOKEN =
            "android.federatedcompute.example_iterator_resumption_token";

    public static final String EXTRA_COLLECTION_URI =
            "android.federatedcompute.collection_uri";
    public static final String EXTRA_EXAMPLE_ITERATOR_RESULT =
            "android.federatedcompute.example_iterator_result";

    // ResultHandlingService related constants.
    public static final String RESULT_HANDLING_SERVICE_ACTION =
            "android.federatedcompute.COMPUTATION_RESULT";

    // ODP mainline signed apex name
    public static final String ODP_MAINLINE_SIGNED_APEX_NAME =
            "com.google.android.ondevicepersonalization";

    // ODP AOSP built apex name
    public static final String ODP_AOSP_BUILT_APEX_NAME =
            "com.android.ondevicepersonalization";

    // ODP apex keyword to capture all other variations of ODP binary (go, other form factors)
    public static final String ODP_APEX_KEYWORD =
            "ondevicepersonalization";

    private ClientConstants() {}
}
