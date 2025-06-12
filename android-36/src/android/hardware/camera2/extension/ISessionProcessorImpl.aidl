/**
 * Copyright (c) 2021, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.hardware.camera2.extension;

import android.hardware.camera2.impl.CameraMetadataNative;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.extension.CameraSessionConfig;
import android.hardware.camera2.extension.ICaptureCallback;
import android.hardware.camera2.extension.IRequestProcessorImpl;
import android.hardware.camera2.extension.LatencyPair;
import android.hardware.camera2.extension.LatencyRange;
import android.hardware.camera2.extension.OutputSurface;

import android.os.IBinder;

/** @hide */
interface ISessionProcessorImpl
{
    CameraSessionConfig initSession(in IBinder token, in String cameraId,
            in Map<String, CameraMetadataNative> charsMap, in OutputSurface previewSurface,
            in OutputSurface imageCaptureSurface, in OutputSurface postviewSurface);
    void deInitSession(in IBinder token);
    void onCaptureSessionStart(IRequestProcessorImpl requestProcessor, in String statsKey);
    void onCaptureSessionEnd();
    int startRepeating(in ICaptureCallback callback);
    void stopRepeating();
    int startCapture(in ICaptureCallback callback, in boolean isPostviewRequested);
    void setParameters(in CaptureRequest captureRequest);
    int startTrigger(in CaptureRequest captureRequest, in ICaptureCallback callback);
    @nullable LatencyPair getRealtimeCaptureLatency();
}
