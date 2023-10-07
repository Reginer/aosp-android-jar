package com.android.clockwork.power;

import android.annotation.Nullable;
import android.content.Context;
import android.hardware.input.InputManager;
import android.util.Log;
import android.view.InputDevice;

import java.util.Arrays;

/** Utility class to provide and cache fresh {@link InputDevice} instances of specific sources. */
class InputDeviceProvider implements InputManager.InputDeviceListener {
    private static final String TAG = "InputDeviceProvider";

    private final InputManager mInputManager;
    private final int mSource;

    @Nullable private EnablableInputDevice mDevice;

    InputDeviceProvider(Context context, int source) {
        mInputManager = context.getSystemService(InputManager.class);
        mSource = source;
        mDevice = getFreshDevice();
    }

    /**
     * Starts listening to device updates from {@link InputManager} and refreshes/caches the latest
     * devices of the target type.
     */
    void startListeningForDeviceUpdates() {
        mInputManager.registerInputDeviceListener(this, /* handler= */ null);
    }

    /**
     * Returns an {@link EnablableInputDevice} of type specified for this instance, or {@code null},
     * if none. Runs in O(1), as this class tracks and caches the latest device.
     */
    @Nullable
    synchronized EnablableInputDevice getDevice() {
        return mDevice;
    }

    @Override
    public synchronized void onInputDeviceAdded(int deviceId) {
        if (mDevice == null) {
            mDevice = getFreshDevice();
        }
    }

    @Override
    public synchronized void onInputDeviceChanged(int deviceId) {
        if (mDevice != null && mDevice.getId() == deviceId) {
            mDevice = getFreshDevice();
        }
    }

    @Override
    public synchronized void onInputDeviceRemoved(int deviceId) {
        if (mDevice != null && mDevice.getId() == deviceId) {
            mDevice = getFreshDevice();
        }
    }

    @Nullable
    private EnablableInputDevice getFreshDevice() {
        int[] deviceIds = mInputManager.getInputDeviceIds();
        if (deviceIds == null) {
            return null;
        }

        return Arrays.stream(deviceIds)
                .mapToObj(mInputManager::getInputDevice)
                .filter(d -> (d.getSources() & mSource) != 0)
                .findFirst()
                .map(EnablableInputDevice::new)
                .orElse(null);
    }
}