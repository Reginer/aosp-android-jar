/*
 * Copyright (C) 2020 The Android Open Source Project
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

package android.service.voice;

import android.service.voice.HotwordDetectedResult;
import android.service.voice.HotwordRejectedResult;

/**
 * Callback for returning the detected result from the HotwordDetectionService.
 *
 * @hide
 */
// TODO: Rename this.
oneway interface IDspHotwordDetectionCallback {

    /**
     * Called when the detected result is valid.
     */
    void onDetected(
        in HotwordDetectedResult hotwordDetectedResult);

    /**
     * Sends {@code result} to the HotwordDetector.
     */
    void onRejected(in HotwordRejectedResult result);
}
