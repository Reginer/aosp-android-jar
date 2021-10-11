/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.setupwizardlib;

import com.android.car.setupwizardlib.InitialLockSetupConstants.ValidateLockFlags;

/**
 * Provides helper methods for the usage of the InitialLockSetupService.
 */
public class InitialLockSetupHelper {

    /**
     * Checks the return flags from a valid lock check and returns true if the lock is valid.
     */
    public static boolean isValidLockResultCode(@ValidateLockFlags int flags) {
        return flags == 0;
    }

    /**
     * Gets the byte representation of a pattern cell based on 0 indexed row and column.
     * This should be a 3x3 pattern format.
     */
    public static byte getByteFromPatternCell(int row, int col) {
        return (byte) ((row * 3) + col + 1);
    }

    /**
     * Returns the 0 indexed row of the pattern cell from a serialized byte pattern cell.
     * This should be a 3x3 pattern format.
     */
    public static int getPatternCellRowFromByte(byte cell) {
        return (byte) ((cell - 1) / 3);
    }

    /**
     * Returns the 0 indexed column of the pattern cell from a serialized byte pattern cell.
     */
    public static int getPatternCellColumnFromByte(byte cell) {
        return (byte) ((cell - 1) % 3);
    }

    /**
     * Converts a {@link CharSequence} into an array of bytes. This is for security reasons to avoid
     * storing strings in memory.
     */
    public static byte[] charSequenceToByteArray(CharSequence chars) {
        if (chars == null) {
            return null;
        }
        byte[] byteArray = new byte[chars.length()];
        for (int i = 0; i < chars.length(); i++) {
            byteArray[i] = (byte) chars.charAt(i);
        }
        return byteArray;
    }

    /**
     * Converts an array of bytes into a {@link CharSequence}.
     */
    public static CharSequence byteArrayToCharSequence(byte[] input) {
        if (input == null) {
            return null;
        }
        StringBuffer charSequence = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            charSequence.append((char) input[i]);
        }
        return charSequence;
    }

    /** Return an ASCII-equivalent array of character digits for a numeric byte input. */
    public static byte[] getNumericEquivalentByteArray(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] + 48);
        }
        return output;
    }
}
