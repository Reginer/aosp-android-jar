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

package android.bluetooth;

import android.os.UserHandle;
import android.util.Log;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@hide}
 */
public final class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";

    /**
     * This utility class cannot be instantiated
     */
    private BluetoothUtils() {}

    /**
     * Timeout value for synchronous binder call
     */
    private static final Duration SYNC_CALLS_TIMEOUT = Duration.ofSeconds(5);

    /**
     * @return timeout value for synchronous binder call
     */
    static Duration getSyncTimeout() {
        return SYNC_CALLS_TIMEOUT;
    }

    /**
     * Match with UserHandl.NULL but accessible inside bluetooth package
     */
    public static final UserHandle USER_HANDLE_NULL = UserHandle.of(-10000);

    static class TypeValueEntry {
        private final int mType;
        private final byte[] mValue;

        TypeValueEntry(int type, byte[] value) {
            mType = type;
            mValue = value;
        }

        public int getType() {
            return mType;
        }

        public byte[] getValue() {
            return mValue;
        }
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] rawBytes, int start, int length) {
        int remainingLength = rawBytes.length - start;
        if (remainingLength < length) {
            Log.w(TAG, "extractBytes() remaining length " + remainingLength
                    + " is less than copying length " + length + ", array length is "
                    + rawBytes.length + " start is " + start);
            return null;
        }
        byte[] bytes = new byte[length];
        System.arraycopy(rawBytes, start, bytes, 0, length);
        return bytes;
    }

    /**
     * Parse Length Value Entry from raw bytes
     *
     * The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
     *
     * @param rawBytes raw bytes of Length-Value-Entry array
     * @hide
     */
    public static List<TypeValueEntry> parseLengthTypeValueBytes(byte[] rawBytes) {
        if (rawBytes == null) {
            return Collections.emptyList();
        }
        if (rawBytes.length == 0) {
            return Collections.emptyList();
        }

        int currentPos = 0;
        List<TypeValueEntry> result = new ArrayList<>();
        while (currentPos < rawBytes.length) {
            // length is unsigned int.
            int length = rawBytes[currentPos] & 0xFF;
            if (length == 0) {
                break;
            }
            currentPos++;
            if (currentPos >= rawBytes.length) {
                Log.w(TAG, "parseLtv() no type and value after length, rawBytes length = "
                        + rawBytes.length + ", currentPost = " + currentPos);
                break;
            }
            // Note the length includes the length of the field type itself.
            int dataLength = length - 1;
            // fieldType is unsigned int.
            int type = rawBytes[currentPos] & 0xFF;
            currentPos++;
            if (currentPos >= rawBytes.length) {
                Log.w(TAG, "parseLtv() no value after length, rawBytes length = "
                        + rawBytes.length + ", currentPost = " + currentPos);
                break;
            }
            byte[] value = extractBytes(rawBytes, currentPos, dataLength);
            if (value == null) {
                Log.w(TAG, "failed to extract bytes, currentPost = " + currentPos);
                break;
            }
            result.add(new TypeValueEntry(type, value));
            currentPos += dataLength;
        }
        return result;
    }

    /**
     * Serialize type value entries to bytes
     * @param typeValueEntries type value entries
     * @return serialized type value entries on success, null on failure
     */
    public static byte[] serializeTypeValue(List<TypeValueEntry> typeValueEntries) {
        // Calculate length
        int length = 0;
        for (TypeValueEntry entry : typeValueEntries) {
            // 1 for length and 1 for type
            length += 2;
            if ((entry.getType() - (entry.getType() & 0xFF)) != 0) {
                Log.w(TAG, "serializeTypeValue() type " + entry.getType()
                        + " is out of range of 0-0xFF");
                return null;
            }
            if (entry.getValue() == null) {
                Log.w(TAG, "serializeTypeValue() value is null");
                return null;
            }
            int lengthValue = entry.getValue().length + 1;
            if ((lengthValue - (lengthValue & 0xFF)) != 0) {
                Log.w(TAG, "serializeTypeValue() entry length "  + entry.getValue().length
                        + " is not in range of 0 to 254");
                return null;
            }
            length += entry.getValue().length;
        }
        byte[] result = new byte[length];
        int currentPos = 0;
        for (TypeValueEntry entry : typeValueEntries) {
            result[currentPos] = (byte) ((entry.getValue().length + 1) & 0xFF);
            currentPos++;
            result[currentPos] = (byte) (entry.getType() & 0xFF);
            currentPos++;
            System.arraycopy(entry.getValue(), 0, result, currentPos, entry.getValue().length);
            currentPos += entry.getValue().length;
        }
        return result;
    }

    /**
     * Convert an address to an obfuscate one for logging purpose
     * @param address Mac address to be log
     * @return Loggable mac address
     */
    public static String toAnonymizedAddress(String address) {
        if (address == null || address.length() != 17) {
            return null;
        }
        return "XX:XX:XX" + address.substring(8);
    }
}
