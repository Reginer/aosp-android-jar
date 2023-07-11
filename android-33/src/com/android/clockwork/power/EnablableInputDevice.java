package com.android.clockwork.power;

import android.view.InputDevice;

import com.android.internal.annotations.VisibleForTesting;

/**
 *Class used to wrap calls to @hide methods on an input device in order to mock for testing.
 */
class EnablableInputDevice {
    private InputDevice mDevice;

    EnablableInputDevice(InputDevice device) {
        mDevice = device;
    }

    @VisibleForTesting
    void enable() {
        mDevice.enable();
    }

    @VisibleForTesting
    void disable() {
        mDevice.disable();
    }

    @VisibleForTesting
    boolean isEnabled() {
        return mDevice.isEnabled();
    }
}
