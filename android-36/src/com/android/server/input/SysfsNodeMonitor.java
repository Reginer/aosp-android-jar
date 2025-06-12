/*
 * Copyright 2025 The Android Open Source Project
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

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UEventObserver;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;

import java.util.Objects;

/**
 * A thread-safe component of {@link InputManagerService} responsible for monitoring the addition
 * of kernel sysfs nodes for newly connected input devices.
 *
 * This class uses the {@link UEventObserver} to monitor for changes to an input device's sysfs
 * nodes, and is responsible for requesting the native code to refresh its sysfs nodes when there
 * is a change. This is necessary because the sysfs nodes may only be configured after an input
 * device is already added, with no way for the native code to detect any changes afterwards.
 */
final class SysfsNodeMonitor {
    private static final String TAG = SysfsNodeMonitor.class.getSimpleName();

    // To enable these logs, run:
    // 'adb shell setprop log.tag.SysfsNodeMonitor DEBUG' (requires restart)
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    private static final long SYSFS_NODE_MONITORING_TIMEOUT_MS = 60_000; // 1 minute

    private final Context mContext;
    private final NativeInputManagerService mNative;
    private final Handler mHandler;
    private final UEventManager mUEventManager;

    private InputManager mInputManager;

    private final SparseArray<SysfsNodeAddedListener> mUEventListenersByDeviceId =
            new SparseArray<>();

    SysfsNodeMonitor(Context context, NativeInputManagerService nativeService, Looper looper,
            UEventManager uEventManager) {
        mContext = context;
        mNative = nativeService;
        mHandler = new Handler(looper);
        mUEventManager = uEventManager;
    }

    public void systemRunning() {
        mInputManager = Objects.requireNonNull(mContext.getSystemService(InputManager.class));
        mInputManager.registerInputDeviceListener(mInputDeviceListener, mHandler);
        for (int deviceId : mInputManager.getInputDeviceIds()) {
            mInputDeviceListener.onInputDeviceAdded(deviceId);
        }
    }

    private final InputManager.InputDeviceListener mInputDeviceListener =
            new InputManager.InputDeviceListener() {
                @Override
                public void onInputDeviceAdded(int deviceId) {
                    startMonitoring(deviceId);
                }

                @Override
                public void onInputDeviceRemoved(int deviceId) {
                    stopMonitoring(deviceId);
                }

                @Override
                public void onInputDeviceChanged(int deviceId) {
                }
            };

    private void startMonitoring(int deviceId) {
        final var inputDevice = mInputManager.getInputDevice(deviceId);
        if (inputDevice == null) {
            return;
        }
        if (!inputDevice.isExternal()) {
            if (DEBUG) {
                Log.d(TAG, "Not listening to sysfs node changes for internal input device: "
                        + deviceId);
            }
            return;
        }
        final var sysfsRootPath = formatDevPath(mNative.getSysfsRootPath(deviceId));
        if (sysfsRootPath == null) {
            if (DEBUG) {
                Log.d(TAG, "Sysfs node not found for external input device: " + deviceId);
            }
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Start listening to sysfs node changes for input device: " + deviceId
                    + ", node: " + sysfsRootPath);
        }
        final var listener = new SysfsNodeAddedListener();
        mUEventListenersByDeviceId.put(deviceId, listener);

        // We must synchronously start monitoring for changes to this device's path.
        // Once monitoring starts, we need to trigger a native refresh of the sysfs nodes to
        // catch any changes that happened between the input device's creation and the UEvent
        // listener being added.
        // NOTE: This relies on the fact that the following `addListener` call is fully synchronous.
        mUEventManager.addListener(listener, sysfsRootPath);
        mNative.sysfsNodeChanged(sysfsRootPath);

        // Always stop listening for new sysfs nodes after the timeout.
        mHandler.postDelayed(() -> stopMonitoring(deviceId), SYSFS_NODE_MONITORING_TIMEOUT_MS);
    }

    private static String formatDevPath(String path) {
        // Remove the "/sys" prefix if it has one.
        return path != null && path.startsWith("/sys") ? path.substring(4) : path;
    }

    private void stopMonitoring(int deviceId) {
        final var listener = mUEventListenersByDeviceId.removeReturnOld(deviceId);
        if (listener == null) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "Stop listening to sysfs node changes for input device: " + deviceId);
        }
        mUEventManager.removeListener(listener);
    }

    class SysfsNodeAddedListener extends UEventManager.UEventListener {

        private boolean mHasReceivedRemovalNotification = false;
        private boolean mHasReceivedPowerSupplyNotification = false;

        @Override
        public void onUEvent(UEventObserver.UEvent event) {
            // This callback happens on the UEventObserver's thread.
            // Ensure we are processing on the handler thread.
            mHandler.post(() -> handleUEvent(event));
        }

        private void handleUEvent(UEventObserver.UEvent event) {
            if (DEBUG) {
                Slog.d(TAG, "UEventListener: Received UEvent: " + event);
            }
            final var subsystem = event.get("SUBSYSTEM");
            final var devPath = "/sys" + Objects.requireNonNull(
                    TextUtils.nullIfEmpty(event.get("DEVPATH")));
            final var action = event.get("ACTION");

            // NOTE: We must be careful to avoid reconfiguring sysfs nodes during device removal,
            // because it might result in the device getting re-opened in native code during
            // removal, resulting in unexpected states. If we see any removal action for this node,
            // ensure we stop responding altogether.
            if (mHasReceivedRemovalNotification || "REMOVE".equalsIgnoreCase(action)) {
                mHasReceivedRemovalNotification = true;
                return;
            }

            if ("LEDS".equalsIgnoreCase(subsystem) && "ADD".equalsIgnoreCase(action)) {
                // An LED node was added. Notify native code to reconfigure the sysfs node.
                if (DEBUG) {
                    Slog.d(TAG,
                            "Reconfiguring sysfs node because 'leds' node was added: " + devPath);
                }
                mNative.sysfsNodeChanged(devPath);
                return;
            }

            if ("POWER_SUPPLY".equalsIgnoreCase(subsystem)) {
                if (mHasReceivedPowerSupplyNotification) {
                    return;
                }
                // This is the first notification we received from the power_supply subsystem.
                // Notify native code that the battery node may have been added. The power_supply
                // subsystem does not seem to be sending ADD events, so use use the first event
                // with any action as a proxy for a new power_supply node being created.
                if (DEBUG) {
                    Slog.d(TAG, "Reconfiguring sysfs node because 'power_supply' node had action '"
                            + action + "': " + devPath);
                }
                mHasReceivedPowerSupplyNotification = true;
                mNative.sysfsNodeChanged(devPath);
            }
        }
    }
}
