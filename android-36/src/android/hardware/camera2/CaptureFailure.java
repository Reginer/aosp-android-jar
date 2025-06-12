/*
 * Copyright (C) 2013 The Android Open Source Project
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
package android.hardware.camera2;

import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A report of failed capture for a single image capture from the image sensor.
 *
 * <p>CaptureFailures are produced by a {@link CameraDevice} if processing a
 * {@link CaptureRequest} fails, either partially or fully. Use {@link #getReason}
 * to determine the specific nature of the failed capture.</p>
 *
 * <p>Receiving a CaptureFailure means that the metadata associated with that frame number
 * has been dropped -- no {@link CaptureResult} with the same frame number will be
 * produced.</p>
 */
public class CaptureFailure {
    /**
     * The {@link CaptureResult} has been dropped this frame only due to an error
     * in the framework.
     *
     * @see #getReason()
     */
    public static final int REASON_ERROR = 0;

    /**
     * The capture has failed due to a {@link CameraCaptureSession#abortCaptures} call from the
     * application.
     *
     * @see #getReason()
     */
    public static final int REASON_FLUSHED = 1;

     /** @hide */
     @Retention(RetentionPolicy.SOURCE)
     @IntDef(prefix = {"REASON_"}, value =
         {REASON_ERROR,
          REASON_FLUSHED })
     public @interface FailureReason {};

    private final CaptureRequest mRequest;
    private final int mReason;
    private final boolean mWasImageCaptured;
    private final int mSequenceId;
    private final long mFrameNumber;
    private final String mErrorPhysicalCameraId;

    /**
     * @hide
     */
    public CaptureFailure(CaptureRequest request, int reason,
            boolean wasImageCaptured, int sequenceId, long frameNumber,
            String errorPhysicalCameraId) {
        mRequest = request;
        mReason = reason;
        mWasImageCaptured = wasImageCaptured;
        mSequenceId = sequenceId;
        mFrameNumber = frameNumber;
        mErrorPhysicalCameraId = errorPhysicalCameraId;
    }

    /**
     * Get the request associated with this failed capture.
     *
     * <p>Whenever a request is unsuccessfully captured, with
     * {@link CameraCaptureSession.CaptureCallback#onCaptureFailed},
     * the {@code failed capture}'s {@code getRequest()} will return that {@code request}.
     * </p>
     *
     * <p>In particular,
     * <code><pre>cameraDevice.capture(someRequest, new CaptureCallback() {
     *     {@literal @}Override
     *     void onCaptureFailed(CaptureRequest myRequest, CaptureFailure myFailure) {
     *         assert(myFailure.getRequest.equals(myRequest) == true);
     *     }
     * };
     * </code></pre>
     * </p>
     *
     * @return The request associated with this failed capture. Never {@code null}.
     */
    @NonNull
    public CaptureRequest getRequest() {
        return mRequest;
    }

    /**
     * Get the frame number associated with this failed capture.
     *
     * <p>Whenever a request has been processed, regardless of failed capture or success,
     * it gets a unique frame number assigned to its future result/failed capture.</p>
     *
     * <p>This value monotonically increments, starting with 0,
     * for every new result or failure; and the scope is the lifetime of the
     * {@link CameraDevice}.</p>
     *
     * @return long frame number
     */
    public long getFrameNumber() {
        return mFrameNumber;
    }

    /**
     * Determine why the request was dropped, whether due to an error or to a user
     * action.
     *
     * @return int The reason code.
     *
     * @see #REASON_ERROR
     * @see #REASON_FLUSHED
     */
    @FailureReason
    public int getReason() {
        return mReason;
    }

    /**
     * Determine if the image was captured from the camera.
     *
     * <p>If the image was not captured, no image buffers will be available.
     * If the image was captured, then image buffers may be available.</p>
     *
     * @return boolean True if the image was captured, false otherwise.
     */
    public boolean wasImageCaptured() {
        return mWasImageCaptured;
    }

    /**
     * The sequence ID for this failed capture that was returned by the
     * {@link CameraCaptureSession#capture} family of functions.
     *
     * <p>The sequence ID is a unique monotonically increasing value starting from 0,
     * incremented every time a new group of requests is submitted to the CameraDevice.</p>
     *
     * @return int The ID for the sequence of requests that this capture failure is the result of
     *
     * @see CameraCaptureSession.CaptureCallback#onCaptureSequenceCompleted
     */
    public int getSequenceId() {
        return mSequenceId;
    }

    /**
     * The physical camera device ID in case the capture failure comes from a {@link CaptureRequest}
     * with configured physical camera streams for a logical camera.
     *
     * @return String The physical camera device ID of the respective failing output.
     *         {@code null} in case the capture request has no associated physical camera device.
     * @see CaptureRequest.Builder#setPhysicalCameraKey
     * @see android.hardware.camera2.params.OutputConfiguration#setPhysicalCameraId
     */
    public @Nullable String getPhysicalCameraId() {
        return mErrorPhysicalCameraId;
    }
}
