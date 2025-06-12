/*
 * Copyright (C) 2014 The Android Open Source Project
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

package android.hardware.hdmi;

import android.annotation.SystemApi;
import android.os.RemoteException;
import android.util.Log;

/**
 * HdmiPlaybackClient represents HDMI-CEC logical device of type Playback
 * in the Android system which acts as a playback device such as set-top box.
 *
 * <p>HdmiPlaybackClient provides methods that control, get information from TV/Display device
 * connected through HDMI bus.
 *
 * @hide
 */
@SystemApi
public final class HdmiPlaybackClient extends HdmiClient {
    private static final String TAG = "HdmiPlaybackClient";

    // Logical address of TV. The secondary TV is not handled.
    private static final int ADDR_TV = 0;

    /**
     * Listener used by the client to get the result of one touch play operation.
     */
    public interface OneTouchPlayCallback {
        /**
         * Called when the result of the feature one touch play is returned.
         *
         * @param result the result of the operation. {@link HdmiControlManager#RESULT_SUCCESS}
         *         if successful.
         */
        public void onComplete(int result);
    }

    /**
     * Listener used by the client to get display device status.
     */
    public interface DisplayStatusCallback {
        /**
         * Called when display device status is reported.
         *
         * @param status display device status. It should be one of the following values.
         *            <ul>
         *            <li>{@link HdmiControlManager#POWER_STATUS_ON}
         *            <li>{@link HdmiControlManager#POWER_STATUS_STANDBY}
         *            <li>{@link HdmiControlManager#POWER_STATUS_TRANSIENT_TO_ON}
         *            <li>{@link HdmiControlManager#POWER_STATUS_TRANSIENT_TO_STANDBY}
         *            <li>{@link HdmiControlManager#POWER_STATUS_UNKNOWN}
         *            </ul>
         */
        public void onComplete(int status);
    }

    /* package */ HdmiPlaybackClient(IHdmiControlService service) {
        super(service);
    }

    /**
     * Performs the feature 'one touch play' from playback device to turn on display and claim
     * to be the streaming source.
     *
     * <p>Client side is responsible to send out intent to choose whichever internal
     * source on the current device it would like to switch to. Otherwise the current
     * device will remain on the current input.
     *
     * @param callback {@link OneTouchPlayCallback} object to get informed
     *         of the result
     */
    public void oneTouchPlay(OneTouchPlayCallback callback) {
        try {
            mService.oneTouchPlay(getCallbackWrapper(callback));
        } catch (RemoteException e) {
            Log.e(TAG, "oneTouchPlay threw exception ", e);
        }
    }

    @Override
    public int getDeviceType() {
        return HdmiDeviceInfo.DEVICE_PLAYBACK;
    }

    /**
     * Gets the status of display device connected through HDMI bus.
     *
     * @param callback {@link DisplayStatusCallback} object to get informed
     *         of the result
     */
    public void queryDisplayStatus(DisplayStatusCallback callback) {
        try {
            mService.queryDisplayStatus(getCallbackWrapper(callback));
        } catch (RemoteException e) {
            Log.e(TAG, "queryDisplayStatus threw exception ", e);
        }
    }

    /**
     * Sends a &lt;Standby&gt; command to TV.
     */
    public void sendStandby() {
        try {
            mService.sendStandby(getDeviceType(), HdmiDeviceInfo.idForCecDevice(ADDR_TV));
        } catch (RemoteException e) {
            Log.e(TAG, "sendStandby threw exception ", e);
        }
    }

    private IHdmiControlCallback getCallbackWrapper(final OneTouchPlayCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("OneTouchPlayCallback cannot be null.");
        }
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int result) {
                callback.onComplete(result);
            }
        };
    }

    private IHdmiControlCallback getCallbackWrapper(final DisplayStatusCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("DisplayStatusCallback cannot be null.");
        }
        return new IHdmiControlCallback.Stub() {
            @Override
            public void onComplete(int status) {
                callback.onComplete(status);
            }
        };
    }
}
