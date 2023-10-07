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

package com.android.clockwork.displayoffload;

import static com.google.android.clockwork.ambient.offload.IDisplayOffloadCallbacks.ERROR_LAYOUT_CONVERSION_FAILURE;

import android.text.TextUtils;

import com.google.android.clockwork.ambient.offload.types.KeyValuePair;
import com.google.common.primitives.Bytes;

import java.util.ArrayList;

class HalTypeConverterUtils {
    static ArrayList<vendor.google_clockwork.displayoffload.V1_0.KeyValuePair> toHalKVPairArrayList(
            KeyValuePair[] keyValuePairs) throws DisplayOffloadException {
        ArrayList<vendor.google_clockwork.displayoffload.V1_0.KeyValuePair> halKVPairArrayList =
                new ArrayList<>();
        for (KeyValuePair aidlKVPair : keyValuePairs) {
            halKVPairArrayList.add(HalTypeConverterUtils.toHalKVPair(aidlKVPair));
        }
        return halKVPairArrayList;
    }

    static vendor.google_clockwork.displayoffload.V1_0.KeyValuePair toHalKVPair(
            KeyValuePair aidlKVPair) throws DisplayOffloadException {
        if (TextUtils.isEmpty(aidlKVPair.key)) {
            throw new DisplayOffloadException(
                    ERROR_LAYOUT_CONVERSION_FAILURE, "Failed to convert: " + aidlKVPair.key);
        }
        vendor.google_clockwork.displayoffload.V1_0.KeyValuePair halKVPair =
                new vendor.google_clockwork.displayoffload.V1_0.KeyValuePair();
        halKVPair.key = aidlKVPair.key;
        halKVPair.value = new ArrayList<>(Bytes.asList(aidlKVPair.value));
        return halKVPair;
    }
}
