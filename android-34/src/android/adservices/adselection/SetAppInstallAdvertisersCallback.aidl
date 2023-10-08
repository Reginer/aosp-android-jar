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

package android.adservices.adselection;

import android.adservices.common.FledgeErrorResponse;

/**
 * This interface defines callback functions for a setAppInstallAdvertisers request,
 * which contain a function to be called upon success that accepts a void argument, as well
 * as a function to be called upon failure that accepts an FledgeErrorResponse argument.
 *
 * @hide
 */
oneway interface SetAppInstallAdvertisersCallback {
    void onSuccess();
    void onFailure(in FledgeErrorResponse responseParcel);
}
