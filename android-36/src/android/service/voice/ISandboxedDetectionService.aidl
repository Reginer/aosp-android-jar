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

import android.content.ContentCaptureOptions;
import android.hardware.soundtrigger.SoundTrigger;
import android.media.AudioFormat;
import android.os.IBinder;
import android.os.IRemoteCallback;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.SharedMemory;
import android.service.voice.IDetectorSessionStorageService;
import android.service.voice.IDetectorSessionVisualQueryDetectionCallback;
import android.service.voice.IDspHotwordDetectionCallback;
import android.view.contentcapture.IContentCaptureManager;
import android.speech.IRecognitionServiceManager;

/**
 * Provide the interface to communicate with sandboxed detection service.
 *
 * @hide
 */
oneway interface ISandboxedDetectionService {
    void detectFromDspSource(
        in SoundTrigger.KeyphraseRecognitionEvent event,
        in AudioFormat audioFormat,
        long timeoutMillis,
        in IDspHotwordDetectionCallback callback);

    void detectFromMicrophoneSource(
        in ParcelFileDescriptor audioStream,
        int audioSource,
        in AudioFormat audioFormat,
        in PersistableBundle options,
        in IDspHotwordDetectionCallback callback);

    void detectWithVisualSignals(in IDetectorSessionVisualQueryDetectionCallback callback);

    void updateState(
        in PersistableBundle options,
        in SharedMemory sharedMemory,
        in IRemoteCallback callback);

    void updateAudioFlinger(in IBinder audioFlinger);

    void updateContentCaptureManager(
        in IContentCaptureManager contentCaptureManager,
        in ContentCaptureOptions options);

    void updateRecognitionServiceManager(
        in IRecognitionServiceManager recognitionServiceManager);

    interface IPingMe {
        void onPing();
    }

    /**
     * Simply requests the service to trigger the callback, so that the system can check its
     * identity.
     */
    void ping(in IPingMe callback);

    void stopDetection();

    /**
     * Registers the interface stub to talk to the voice interaction service for initialization/
     * detection unrelated functionalities.
     */
    void registerRemoteStorageService(in IDetectorSessionStorageService detectorSessionStorageService);
}
