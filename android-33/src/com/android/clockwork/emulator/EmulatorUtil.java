package com.android.clockwork.emulator;

import android.os.Build;

/**
 * Util to query Wear emulator state.
 *
 * <p>Keep in sync with {@link com.google.android.clockwork.common.emulator.EmulatorUtil}
 */
public final class EmulatorUtil {
    private EmulatorUtil() {}

    public static boolean isEmulator() {
        return Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.HARDWARE.contains("cutf_cvm");
    }
}
