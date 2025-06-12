/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.Manifest.permission.CAPTURE_AUDIO_HOTWORD;
import static android.Manifest.permission.RECORD_AUDIO;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.SystemApi;
import android.media.AudioFormat;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.SharedMemory;

import java.io.PrintWriter;

/**
 * Basic functionality for sandboxed detectors. This interface will be used by detectors that
 * manages their service lifecycle.
 *
 * @hide
 */
@SystemApi
public interface HotwordDetector {

    /**
     * Indicates that it is a non-trusted hotword detector.
     *
     * @hide
     */
    int DETECTOR_TYPE_NORMAL = 0;

    /**
     * Indicates that it is a DSP trusted hotword detector.
     *
     * @hide
     */
    int DETECTOR_TYPE_TRUSTED_HOTWORD_DSP = 1;

    /**
     * Indicates that it is a software trusted hotword detector.
     *
     * @hide
     */
    int DETECTOR_TYPE_TRUSTED_HOTWORD_SOFTWARE = 2;

    /**
     * Indicates that it is a visual query detector.
     *
     * @hide
     */
    int DETECTOR_TYPE_VISUAL_QUERY_DETECTOR = 3;

    /**
     * Starts sandboxed detection recognition.
     * <p>
     * If a {@link VisualQueryDetector} calls this method, {@link VisualQueryDetectionService
     * #onStartDetection(VisualQueryDetectionService.Callback)} will be called to start detection.
     * <p>
     * Otherwise if a {@link AlwaysOnHotwordDetector} or {@link SoftwareHotwordDetector} calls this,
     * the system streams audio from the device microphone to this application's
     * {@link HotwordDetectionService}. Audio is streamed until {@link #stopRecognition()} is
     * called.
     * <p>
     * On detection of a hotword,
     * {@link AlwaysOnHotwordDetector.Callback#onDetected(AlwaysOnHotwordDetector.EventPayload)}
     * is called on the callback provided when creating this {@link HotwordDetector}.
     * <p>
     * There is a noticeable impact on battery while recognition is active, so make sure to call
     * {@link #stopRecognition()} when detection isn't needed.
     * <p>
     * Calling this again while recognition is active does nothing.
     *
     * @return {@code true} if the request to start recognition succeeded
     */
    @RequiresPermission(allOf = {RECORD_AUDIO, CAPTURE_AUDIO_HOTWORD})
    boolean startRecognition();

    /**
     * Stops sandboxed detection recognition.
     *
     * @return {@code true} if the request to stop recognition succeeded
     */
    boolean stopRecognition();

    /**
     * Starts hotword recognition on audio coming from an external connected microphone.
     * <p>
     * {@link #stopRecognition()} must be called before {@code audioStream} is closed.
     *
     * @param audioStream stream containing the audio bytes to run detection on
     * @param audioFormat format of the encoded audio
     * @param options options supporting detection, such as configuration specific to the
     *         source of the audio. This will be provided to the {@link HotwordDetectionService}.
     *         PersistableBundle does not allow any remotable objects or other contents that can be
     *         used to communicate with other processes.
     * @return {@code true} if the request to start recognition succeeded
     */
    boolean startRecognition(
            @NonNull ParcelFileDescriptor audioStream,
            @NonNull AudioFormat audioFormat,
            @Nullable PersistableBundle options);

    /**
     * Set configuration and pass read-only data to sandboxed detection service.
     *
     * @param options Application configuration data to provide to sandboxed detection services.
     * PersistableBundle does not allow any remotable objects or other contents that can be used to
     * communicate with other processes.
     * @param sharedMemory The unrestricted data blob to provide to sandboxed detection services.
     * Use this to provide model data or other such data to the trusted process.
     * @throws IllegalStateException if this HotwordDetector wasn't specified to use a
     *         sandboxed detection service when it was created.
     */
    void updateState(@Nullable PersistableBundle options, @Nullable SharedMemory sharedMemory);

    /**
     * Invalidates this detector so that any future calls to this result
     * in an {@link IllegalStateException} when a caller has a target SDK below API level 33
     * or an {@link IllegalDetectorStateException} when a caller has a target SDK of API level 33
     * or above.
     *
     * <p>If there are no other {@link HotwordDetector} instances linked to the
     * sandboxed detection service, the service will be shutdown.
     */
    default void destroy() {
        throw new UnsupportedOperationException("Not implemented. Must override in a subclass.");
    }

    /**
     * @hide
     */
    default boolean isUsingSandboxedDetectionService() {
        throw new UnsupportedOperationException("Not implemented. Must override in a subclass.");
    }

    /**
     * @hide
     */
    static String detectorTypeToString(int detectorType) {
        switch (detectorType) {
            case DETECTOR_TYPE_NORMAL:
                return "normal";
            case DETECTOR_TYPE_TRUSTED_HOTWORD_DSP:
                return "trusted_hotword_dsp";
            case DETECTOR_TYPE_TRUSTED_HOTWORD_SOFTWARE:
                return "trusted_hotword_software";
            case DETECTOR_TYPE_VISUAL_QUERY_DETECTOR:
                return "visual_query_detector";
            default:
                return Integer.toString(detectorType);
        }
    }

    /** @hide */
    default void dump(String prefix, PrintWriter pw) {
        throw new UnsupportedOperationException("Not implemented. Must override in a subclass.");
    }

    /**
     * The callback to notify of detection events.
     */
    interface Callback {

        /**
         * Called when the keyphrase is spoken.
         *
         * @param eventPayload Payload data for the detection event.
         */
        // TODO: Consider creating a new EventPayload that the AOHD one subclasses.
        void onDetected(@NonNull AlwaysOnHotwordDetector.EventPayload eventPayload);

        /**
         * Called when the detection fails due to an error.
         *
         * @deprecated On {@link android.os.Build.VERSION_CODES#UPSIDE_DOWN_CAKE} and above,
         * implement {@link HotwordDetector.Callback#onFailure(HotwordDetectionServiceFailure)},
         * {@link AlwaysOnHotwordDetector.Callback#onFailure(SoundTriggerFailure)},
         * {@link HotwordDetector.Callback#onUnknownFailure(String)} instead.
         */
        @Deprecated
        void onError();

        /**
         * Called when the detection fails due to an error occurs in the
         * {@link HotwordDetectionService}, {@link HotwordDetectionServiceFailure} will be reported
         * to the detector.
         *
         * @param hotwordDetectionServiceFailure It provides the error code, error message and
         *                                       suggested action.
         */
        default void onFailure(
                @NonNull HotwordDetectionServiceFailure hotwordDetectionServiceFailure) {
            onError();
        }

        /**
         * Called when the detection fails due to an unknown error occurs, an error message
         * will be reported to the detector.
         *
         * @param errorMessage It provides the error message.
         */
        default void onUnknownFailure(@NonNull String errorMessage) {
            onError();
        }

        /**
         * Called when the recognition is paused temporarily for some reason.
         * This is an informational callback, and the clients shouldn't be doing anything here
         * except showing an indication on their UI if they have to.
         */
        void onRecognitionPaused();

        /**
         * Called when the recognition is resumed after it was temporarily paused.
         * This is an informational callback, and the clients shouldn't be doing anything here
         * except showing an indication on their UI if they have to.
         */
        void onRecognitionResumed();

        /**
         * Called when the {@link HotwordDetectionService} second stage detection did not detect the
         * keyphrase.
         *
         * @param result Info about the second stage detection result, provided by the
         *         {@link HotwordDetectionService}.
         */
        void onRejected(@NonNull HotwordRejectedResult result);

        /**
         * Called when the {@link HotwordDetectionService} or {@link VisualQueryDetectionService} is
         * created by the system and given a short amount of time to report their initialization
         * state.
         *
         * @param status Info about initialization state of {@link HotwordDetectionService} or
         * {@link VisualQueryDetectionService}; allowed values are
         * {@link SandboxedDetectionInitializer#INITIALIZATION_STATUS_SUCCESS},
         * 1<->{@link SandboxedDetectionInitializer#getMaxCustomInitializationStatus()},
         * {@link SandboxedDetectionInitializer#INITIALIZATION_STATUS_UNKNOWN}.
         */
        void onHotwordDetectionServiceInitialized(int status);

        /**
         * Called with the {@link HotwordDetectionService} or {@link VisualQueryDetectionService} is
         * restarted.
         *
         * Clients are expected to call {@link HotwordDetector#updateState} to share the state with
         * the newly created service.
         */
        void onHotwordDetectionServiceRestarted();
    }
}
