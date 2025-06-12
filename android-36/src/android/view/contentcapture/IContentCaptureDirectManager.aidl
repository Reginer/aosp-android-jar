/*
 * Copyright (C) 2018 The Android Open Source Project
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

package android.view.contentcapture;

import android.content.pm.ParceledListSlice;
import android.view.contentcapture.ContentCaptureEvent;
import android.content.ContentCaptureOptions;

/**
  * Interface between an app (ContentCaptureManager / ContentCaptureSession) and the app providing
  * the ContentCaptureService implementation.
  *
  * @hide
  */
oneway interface IContentCaptureDirectManager {
    // reason and options are used only for metrics logging.
    void sendEvents(in ParceledListSlice events, int reason, in ContentCaptureOptions options);
}
