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

package com.android.server.input;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.hardware.display.DisplayTopologyGraph;
import android.hardware.display.DisplayViewport;
import android.hardware.input.InputSensorInfo;
import android.hardware.lights.Light;
import android.os.IBinder;
import android.os.MessageQueue;
import android.util.SparseArray;
import android.view.InputApplicationHandle;
import android.view.InputChannel;
import android.view.InputEvent;
import android.view.PointerIcon;
import android.view.VerifiedInputEvent;

import java.util.List;

/**
 * An interface for the native methods of InputManagerService. We use a public interface so that
 * this can be mocked for testing by Mockito.
 */
interface NativeInputManagerService {

    void start();

    void setDisplayViewports(DisplayViewport[] viewports);

    void setDisplayTopology(DisplayTopologyGraph topologyGraph);

    int getScanCodeState(int deviceId, int sourceMask, int scanCode);

    int getKeyCodeState(int deviceId, int sourceMask, int keyCode);

    int getSwitchState(int deviceId, int sourceMask, int sw);

    void setKeyRemapping(int[] fromKeyCodes, int[] toKeyCodes);

    boolean hasKeys(int deviceId, int sourceMask, int[] keyCodes, boolean[] keyExists);

    int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode);

    InputChannel createInputChannel(String name);

    InputChannel createInputMonitor(int displayId, String name, int pid);

    void removeInputChannel(IBinder connectionToken);

    void pilferPointers(IBinder token);

    void setInputFilterEnabled(boolean enable);

    /**
     * Set the touch mode state for the display passed as argument.
     *
     * @param inTouchMode   true if the device is in touch mode
     * @param pid           the pid of the process that requested to switch touch mode state
     * @param uid           the uid of the process that requested to switch touch mode state
     * @param hasPermission if set to {@code true} then no further authorization will be performed
     * @param displayId     the target display (ignored if device is configured with per display
     *                      touch mode enabled)
     * @return {@code true} if the touch mode was successfully changed, {@code false} otherwise
     */
    boolean setInTouchMode(boolean inTouchMode, int pid, int uid, boolean hasPermission,
            int displayId);

    void setMaximumObscuringOpacityForTouch(float opacity);

    /**
     * Inject an input event into the system.
     *
     * @param event         the input event to inject
     * @param injectIntoUid true if the event should target windows owned by uid, false otherwise
     * @param uid           the uid whose windows should be targeted, if any
     * @param syncMode      {@link android.os.InputEventInjectionSync}
     * @param timeoutMillis timeout to wait for input injection to complete, in milliseconds
     * @param policyFlags   defined in {@link android.view.WindowManagerPolicyConstants}
     * @return {@link android.os.InputEventInjectionResult}
     */
    int injectInputEvent(InputEvent event, boolean injectIntoUid, int uid, int syncMode,
            int timeoutMillis, int policyFlags);

    VerifiedInputEvent verifyInputEvent(InputEvent event);

    void toggleCapsLock(int deviceId);

    void resetLockedModifierState();

    void displayRemoved(int displayId);

    void setInputDispatchMode(boolean enabled, boolean frozen);

    void setSystemUiLightsOut(boolean lightsOut);

    void setFocusedApplication(int displayId, InputApplicationHandle application);

    void setFocusedDisplay(int displayId);

    void setMinTimeBetweenUserActivityPokes(long millis);

    boolean transferTouchGesture(IBinder fromChannelToken, IBinder toChannelToken,
            boolean isDragDrop, boolean transferEntireGesture);

    /**
     * Transfer the current touch gesture to the window identified by 'destChannelToken' positioned
     * on display with id 'displayId'.
     * @deprecated Use {@link #transferTouchGesture(IBinder, IBinder, boolean)}
     */
    @Deprecated
    boolean transferTouch(IBinder destChannelToken, int displayId);

    int getMousePointerSpeed();

    void setPointerSpeed(int speed);

    void setMouseScalingEnabled(int displayId, boolean enabled);

    void setMouseReverseVerticalScrollingEnabled(boolean enabled);

    void setMouseScrollingAccelerationEnabled(boolean enabled);

    void setMouseScrollingSpeed(int speed);

    void setMouseSwapPrimaryButtonEnabled(boolean enabled);

    void setMouseAccelerationEnabled(boolean enabled);

    void setTouchpadPointerSpeed(int speed);

    void setTouchpadNaturalScrollingEnabled(boolean enabled);

    void setTouchpadTapToClickEnabled(boolean enabled);

    void setTouchpadTapDraggingEnabled(boolean enabled);

    void setShouldNotifyTouchpadHardwareState(boolean enabled);

    void setTouchpadRightClickZoneEnabled(boolean enabled);

    void setTouchpadThreeFingerTapShortcutEnabled(boolean enabled);

    void setTouchpadSystemGesturesEnabled(boolean enabled);

    void setTouchpadAccelerationEnabled(boolean enabled);

    void setShowTouches(boolean enabled);

    void setNonInteractiveDisplays(int[] displayIds);

    void reloadCalibration();

    void vibrate(int deviceId, long[] pattern, int[] amplitudes, int repeat, int token);

    void vibrateCombined(int deviceId, long[] pattern, SparseArray<int[]> amplitudes,
            int repeat, int token);

    void cancelVibrate(int deviceId, int token);

    boolean isVibrating(int deviceId);

    int[] getVibratorIds(int deviceId);

    int getBatteryCapacity(int deviceId);

    int getBatteryStatus(int deviceId);

    /**
     * Get the device path of the battery for an input device.
     * @return the path for the input device battery, or null if there is none.
     */
    @Nullable
    String getBatteryDevicePath(int deviceId);

    List<Light> getLights(int deviceId);

    int getLightPlayerId(int deviceId, int lightId);

    int getLightColor(int deviceId, int lightId);

    void setLightPlayerId(int deviceId, int lightId, int playerId);

    void setLightColor(int deviceId, int lightId, int color);

    void reloadKeyboardLayouts();

    void reloadDeviceAliases();

    String dump();

    void monitor();

    void enableInputDevice(int deviceId);

    void disableInputDevice(int deviceId);

    void reloadPointerIcons();

    boolean setPointerIcon(@NonNull PointerIcon icon, int displayId, int deviceId, int pointerId,
            @NonNull IBinder inputToken);

    void setPointerIconVisibility(int displayId, boolean visible);

    void requestPointerCapture(IBinder windowToken, boolean enabled);

    boolean canDispatchToDisplay(int deviceId, int displayId);

    void notifyPortAssociationsChanged();

    void changeUniqueIdAssociation();

    void changeTypeAssociation();

    void changeKeyboardLayoutAssociation();

    void setDisplayEligibilityForPointerCapture(int displayId, boolean enabled);

    void setMotionClassifierEnabled(boolean enabled);

    void setKeyRepeatConfiguration(int timeoutMs, int delayMs, boolean keyRepeatEnabled);

    InputSensorInfo[] getSensorList(int deviceId);

    @Nullable
    TouchpadHardwareProperties getTouchpadHardwareProperties(int deviceId);

    boolean flushSensor(int deviceId, int sensorType);

    boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs,
            int maxBatchReportLatencyUs);

    void disableSensor(int deviceId, int sensorType);

    void cancelCurrentTouch();

    /** Set the displayId on which the mouse cursor should be shown. */
    void setPointerDisplayId(int displayId);

    /** Get the bluetooth address of an input device if known, otherwise return null. */
    String getBluetoothAddress(int deviceId);

    /** Set whether stylus button reporting through motion events should be enabled. */
    void setStylusButtonMotionEventsEnabled(boolean enabled);

    /**
     * Get the current position of the mouse cursor on the given display.
     *
     * If the mouse cursor is not currently shown, the coordinate values will be NaN-s. Use
     * {@link android.view.Display#INVALID_DISPLAY} to get the position of the default mouse cursor.
     *
     * NOTE: This will grab the PointerController's lock, so we must be careful about calling this
     * from the InputReader or Display threads, which may result in a deadlock.
     */
    float[] getMouseCursorPosition(int displayId);

    /** Set whether showing a pointer icon for styluses is enabled. */
    void setStylusPointerIconEnabled(boolean enabled);

    /** Get the sysfs root path of an input device if known, otherwise return null. */
    @Nullable String getSysfsRootPath(int deviceId);

    /**
     * Report sysfs node changes. This may result in recreation of the corresponding InputDevice.
     * The recreated device may contain new associated peripheral devices like Light, Battery, etc.
     */
    void sysfsNodeChanged(String sysfsNodePath);

    /**
     * Notify if Accessibility bounce keys threshold is changed from InputSettings.
     */
    void setAccessibilityBounceKeysThreshold(int thresholdTimeMs);

    /**
     * Notify if Accessibility slow keys threshold is changed from InputSettings.
     */
    void setAccessibilitySlowKeysThreshold(int thresholdTimeMs);

    /**
     * Notify if Accessibility sticky keys is enabled/disabled from InputSettings.
     */
    void setAccessibilityStickyKeysEnabled(boolean enabled);

    void setInputMethodConnectionIsActive(boolean isActive);

    /**
     * Get the device ID of the InputDevice that used most recently.
     *
     * @return the last used input device ID, or
     *     {@link android.os.IInputConstants#INVALID_INPUT_DEVICE_ID} if no device has been used
     *     since boot.
     */
    int getLastUsedInputDeviceId();

    /**
     * Set whether the given input device can wake up the kernel from sleep
     * when it generates input events. By default, usually only internal (built-in)
     * input devices can wake the kernel from sleep. For an external input device
     * that supports remote wakeup to be able to wake the kernel, this must be called
     * after each time the device is connected/added.
     *
     * Returns true if setting power wakeup was successful.
     */
    boolean setKernelWakeEnabled(int deviceId, boolean enabled);

    /**
     * Set whether the accessibility pointer motion filter is enabled.
     * <p>
     * Once enabled, {@link InputManagerService#filterPointerMotion} is called for evety motion
     * event from pointer devices.
     *
     * @param enabled {@code true} if the filter is enabled, {@code false} otherwise.
     */
    void setAccessibilityPointerMotionFilterEnabled(boolean enabled);

    /** The native implementation of InputManagerService methods. */
    class NativeImpl implements NativeInputManagerService {
        /** Pointer to native input manager service object, used by native code. */
        @SuppressWarnings({"unused", "FieldCanBeLocal"})
        private final long mPtr;

        NativeImpl(InputManagerService service, MessageQueue messageQueue) {
            mPtr = init(service, messageQueue);
        }

        private native long init(InputManagerService service, MessageQueue messageQueue);

        @Override
        public native void start();

        @Override
        public native void setDisplayViewports(DisplayViewport[] viewports);

        @Override
        public native void setDisplayTopology(DisplayTopologyGraph topologyGraph);

        @Override
        public native int getScanCodeState(int deviceId, int sourceMask, int scanCode);

        @Override
        public native int getKeyCodeState(int deviceId, int sourceMask, int keyCode);

        @Override
        public native int getSwitchState(int deviceId, int sourceMask, int sw);

        @Override
        public native void setKeyRemapping(int[] fromKeyCodes, int[] toKeyCodes);

        @Override
        public native boolean hasKeys(int deviceId, int sourceMask, int[] keyCodes,
                boolean[] keyExists);

        @Override
        public native int getKeyCodeForKeyLocation(int deviceId, int locationKeyCode);

        @Override
        public native InputChannel createInputChannel(String name);

        @Override
        public native InputChannel createInputMonitor(int displayId, String name, int pid);

        @Override
        public native void removeInputChannel(IBinder connectionToken);

        @Override
        public native void pilferPointers(IBinder token);

        @Override
        public native void setInputFilterEnabled(boolean enable);

        @Override
        public native boolean setInTouchMode(boolean inTouchMode, int pid, int uid,
                boolean hasPermission, int displayId);

        @Override
        public native void setMaximumObscuringOpacityForTouch(float opacity);

        @Override
        public native int injectInputEvent(InputEvent event, boolean injectIntoUid, int uid,
                int syncMode, int timeoutMillis, int policyFlags);

        @Override
        public native VerifiedInputEvent verifyInputEvent(InputEvent event);

        @Override
        public native void toggleCapsLock(int deviceId);

        @Override
        public native void resetLockedModifierState();

        @Override
        public native void displayRemoved(int displayId);

        @Override
        public native void setInputDispatchMode(boolean enabled, boolean frozen);

        @Override
        public native void setSystemUiLightsOut(boolean lightsOut);

        @Override
        public native void setFocusedApplication(int displayId, InputApplicationHandle application);

        @Override
        public native void setFocusedDisplay(int displayId);

        @Override
        public native void setMinTimeBetweenUserActivityPokes(long millis);

        @Override
        public native boolean transferTouchGesture(IBinder fromChannelToken, IBinder toChannelToken,
                boolean isDragDrop, boolean transferEntireGesture);

        @Override
        @Deprecated
        public native boolean transferTouch(IBinder destChannelToken, int displayId);

        @Override
        public native int getMousePointerSpeed();

        @Override
        public native void setPointerSpeed(int speed);

        @Override
        public native void setMouseScalingEnabled(int displayId, boolean enabled);

        @Override
        public native void setMouseReverseVerticalScrollingEnabled(boolean enabled);

        @Override
        public native void setMouseScrollingAccelerationEnabled(boolean enabled);

        @Override
        public native void setMouseScrollingSpeed(int speed);

        @Override
        public native void setMouseSwapPrimaryButtonEnabled(boolean enabled);

        @Override
        public native void setMouseAccelerationEnabled(boolean enabled);

        @Override
        public native void setTouchpadPointerSpeed(int speed);

        @Override
        public native void setTouchpadNaturalScrollingEnabled(boolean enabled);

        @Override
        public native void setTouchpadTapToClickEnabled(boolean enabled);

        @Override
        public native void setTouchpadTapDraggingEnabled(boolean enabled);

        @Override
        public native void setShouldNotifyTouchpadHardwareState(boolean enabled);

        @Override
        public native void setTouchpadRightClickZoneEnabled(boolean enabled);

        @Override
        public native void setTouchpadThreeFingerTapShortcutEnabled(boolean enabled);

        @Override
        public native void setTouchpadSystemGesturesEnabled(boolean enabled);

        @Override
        public native void setTouchpadAccelerationEnabled(boolean enabled);

        @Override
        public native void setShowTouches(boolean enabled);

        @Override
        public native void setNonInteractiveDisplays(int[] displayIds);

        @Override
        public native void reloadCalibration();

        @Override
        public native void vibrate(int deviceId, long[] pattern, int[] amplitudes, int repeat,
                int token);

        @Override
        public native void vibrateCombined(int deviceId, long[] pattern,
                SparseArray<int[]> amplitudes,
                int repeat, int token);

        @Override
        public native void cancelVibrate(int deviceId, int token);

        @Override
        public native boolean isVibrating(int deviceId);

        @Override
        public native int[] getVibratorIds(int deviceId);

        @Override
        public native int getBatteryCapacity(int deviceId);

        @Override
        public native int getBatteryStatus(int deviceId);

        @Override
        public native String getBatteryDevicePath(int deviceId);

        @Override
        public native List<Light> getLights(int deviceId);

        @Override
        public native int getLightPlayerId(int deviceId, int lightId);

        @Override
        public native int getLightColor(int deviceId, int lightId);

        @Override
        public native void setLightPlayerId(int deviceId, int lightId, int playerId);

        @Override
        public native void setLightColor(int deviceId, int lightId, int color);

        @Override
        public native void reloadKeyboardLayouts();

        @Override
        public native void reloadDeviceAliases();

        @Override
        public native String dump();

        @Override
        public native void monitor();

        @Override
        public native void enableInputDevice(int deviceId);

        @Override
        public native void disableInputDevice(int deviceId);

        @Override
        public native void reloadPointerIcons();

        @Override
        public native boolean setPointerIcon(PointerIcon icon, int displayId, int deviceId,
                int pointerId, IBinder inputToken);

        @Override
        public native void setPointerIconVisibility(int displayId, boolean visible);

        @Override
        public native void requestPointerCapture(IBinder windowToken, boolean enabled);

        @Override
        public native boolean canDispatchToDisplay(int deviceId, int displayId);

        @Override
        public native void notifyPortAssociationsChanged();

        @Override
        public native void changeUniqueIdAssociation();

        @Override
        public native void changeTypeAssociation();

        @Override
        public native void changeKeyboardLayoutAssociation();

        @Override
        public native void setDisplayEligibilityForPointerCapture(int displayId, boolean enabled);

        @Override
        public native void setMotionClassifierEnabled(boolean enabled);

        @Override
        public native void setKeyRepeatConfiguration(int timeoutMs, int delayMs,
                boolean keyRepeatEnabled);

        @Override
        public native InputSensorInfo[] getSensorList(int deviceId);

        @Override
        public native TouchpadHardwareProperties getTouchpadHardwareProperties(int deviceId);

        @Override
        public native boolean flushSensor(int deviceId, int sensorType);

        @Override
        public native boolean enableSensor(int deviceId, int sensorType, int samplingPeriodUs,
                int maxBatchReportLatencyUs);

        @Override
        public native void disableSensor(int deviceId, int sensorType);

        @Override
        public native void cancelCurrentTouch();

        @Override
        public native void setPointerDisplayId(int displayId);

        @Override
        public native String getBluetoothAddress(int deviceId);

        @Override
        public native void setStylusButtonMotionEventsEnabled(boolean enabled);

        @Override
        public native float[] getMouseCursorPosition(int displayId);

        @Override
        public native void setStylusPointerIconEnabled(boolean enabled);

        @Override
        public native String getSysfsRootPath(int deviceId);

        @Override
        public native void sysfsNodeChanged(String sysfsNodePath);

        @Override
        public native void setAccessibilityBounceKeysThreshold(int thresholdTimeMs);

        @Override
        public native void setAccessibilitySlowKeysThreshold(int thresholdTimeMs);

        @Override
        public native void setAccessibilityStickyKeysEnabled(boolean enabled);

        @Override
        public native void setInputMethodConnectionIsActive(boolean isActive);

        @Override
        public native int getLastUsedInputDeviceId();

        @Override
        public native boolean setKernelWakeEnabled(int deviceId, boolean enabled);

        @Override
        public native void setAccessibilityPointerMotionFilterEnabled(boolean enabled);
    }
}
