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
import android.util.AndroidException;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * <p><code>CameraAccessException</code> is thrown if a camera device could not
 * be queried or opened by the {@link CameraManager}, or if the connection to an
 * opened {@link CameraDevice} is no longer valid.</p>
 *
 * @see CameraManager
 * @see CameraDevice
 */
public class CameraAccessException extends AndroidException {
    /**
     * The camera device is in use already.
     */
    public static final int CAMERA_IN_USE = 4;

    /**
     * The system-wide limit for number of open cameras or camera resources has
     * been reached, and more camera devices cannot be opened or torch mode
     * cannot be turned on until previous instances are closed.
     */
    public static final int MAX_CAMERAS_IN_USE = 5;

    /**
     * The camera is disabled due to a device policy, and cannot be opened.
     *
     * @see android.app.admin.DevicePolicyManager#setCameraDisabled(android.content.ComponentName, boolean)
     */
    public static final int CAMERA_DISABLED = 1;

    /**
     * The camera device is removable and has been disconnected from the Android
     * device, or the camera id used with {@link android.hardware.camera2.CameraManager#openCamera}
     * is no longer valid, or the camera service has shut down the connection due to a
     * higher-priority access request for the camera device.
     */
    public static final int CAMERA_DISCONNECTED = 2;

    /**
     * The camera device is currently in the error state.
     *
     * <p>The camera has failed to open or has failed at a later time
     * as a result of some non-user interaction. Refer to
     * {@link CameraDevice.StateCallback#onError} for the exact
     * nature of the error.</p>
     *
     * <p>No further calls to the camera will succeed. Clean up
     * the camera with {@link CameraDevice#close} and try
     * handling the error in order to successfully re-open the camera.
     * </p>
     *
     */
    public static final int CAMERA_ERROR = 3;

    /**
     * A deprecated HAL version is in use.
     * @hide
     */
    public static final int CAMERA_DEPRECATED_HAL = 1000;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "CAMERA_", "MAX_CAMERAS_IN_USE" }, value = {
            CAMERA_IN_USE,
            MAX_CAMERAS_IN_USE,
            CAMERA_DISABLED,
            CAMERA_DISCONNECTED,
            CAMERA_ERROR
    })
    public @interface AccessError {}

    // Make the eclipse warning about serializable exceptions go away
    private static final long serialVersionUID = 5630338637471475675L; // randomly generated

    private final int mReason;

    /**
     * The reason for the failure to access the camera.
     *
     * @see #CAMERA_DISABLED
     * @see #CAMERA_DISCONNECTED
     * @see #CAMERA_ERROR
     */
    @AccessError
    public final int getReason() {
        return mReason;
    }

    public CameraAccessException(@AccessError int problem) {
        super(getDefaultMessage(problem));
        mReason = problem;
    }

    public CameraAccessException(@AccessError int problem, String message) {
        super(getCombinedMessage(problem, message));
        mReason = problem;
    }

    public CameraAccessException(@AccessError int problem, String message, Throwable cause) {
        super(getCombinedMessage(problem, message), cause);
        mReason = problem;
    }

    public CameraAccessException(@AccessError int problem, Throwable cause) {
        super(getDefaultMessage(problem), cause);
        mReason = problem;
    }

    /**
     * @hide
     */
    public static String getDefaultMessage(@AccessError int problem) {
        switch (problem) {
            case CAMERA_IN_USE:
                return "The camera device is in use already";
            case MAX_CAMERAS_IN_USE:
                return "The system-wide limit for number of open cameras has been reached, " +
                       "and more camera devices cannot be opened until previous instances " +
                       "are closed.";
            case CAMERA_DISCONNECTED:
                return "The camera device is removable and has been disconnected from the " +
                        "Android device, or the camera service has shut down the connection due " +
                        "to a higher-priority access request for the camera device.";
            case CAMERA_DISABLED:
                return "The camera is disabled due to a device policy, and cannot be opened.";
            case CAMERA_ERROR:
                return "The camera device is currently in the error state; " +
                       "no further calls to it will succeed.";
        }
        return null;
    }

    private static String getCombinedMessage(@AccessError int problem, String message) {
        String problemString = getProblemString(problem);
        return String.format("%s (%d): %s", problemString, problem, message);
    }

    private static String getProblemString(int problem) {
        String problemString;
        switch (problem) {
            case CAMERA_IN_USE:
                problemString = "CAMERA_IN_USE";
                break;
            case MAX_CAMERAS_IN_USE:
                problemString = "MAX_CAMERAS_IN_USE";
                break;
            case CAMERA_DISCONNECTED:
                problemString = "CAMERA_DISCONNECTED";
                break;
            case CAMERA_DISABLED:
                problemString = "CAMERA_DISABLED";
                break;
            case CAMERA_ERROR:
                problemString = "CAMERA_ERROR";
                break;
            case CAMERA_DEPRECATED_HAL:
                problemString = "CAMERA_DEPRECATED_HAL";
                break;
            default:
                problemString = "<UNKNOWN ERROR>";
        }
        return problemString;
    }

}
