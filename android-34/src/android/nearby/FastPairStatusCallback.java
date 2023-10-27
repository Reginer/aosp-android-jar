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

package android.nearby;

import android.annotation.NonNull;

/**
 * Reports the pair status for an ongoing pair with a {@link FastPairDevice}.
 * @hide
 */
public interface FastPairStatusCallback {

    /** Reports a pair status related metadata associated with a {@link FastPairDevice} */
    void onPairUpdate(@NonNull FastPairDevice fastPairDevice,
            PairStatusMetadata pairStatusMetadata);
}
