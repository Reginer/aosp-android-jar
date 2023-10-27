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

    public static final String EXTRA_COLLECTION_NAME = "android.federatedcompute.collection_name";

    // ExampleStoreService related constants.
    public static final String EXAMPLE_STORE_ACTION = "android.federatedcompute.EXAMPLE_STORE";
    public static final String EXTRA_EXAMPLE_ITERATOR_CRITERIA =
            "android.federatedcompute.example_iterator_criteria";
    public static final String EXTRA_EXAMPLE_ITERATOR_RESUMPTION_TOKEN =
            "android.federatedcompute.example_iterator_resumption_token";
    public static final String EXTRA_EXAMPLE_ITERATOR_RESULT =
            "android.federatedcompute.example_iterator_result";

    // ResultHandlingService related constants.
    public static final String RESULT_HANDLING_SERVICE_ACTION =
            "android.federatedcompute.COMPUTATION_RESULT";

    private ClientConstants() {}
}
