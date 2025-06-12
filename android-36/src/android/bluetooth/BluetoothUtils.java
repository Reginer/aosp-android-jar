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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresNoPermission;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/** @hide */
public final class BluetoothUtils {
    private static final String TAG = "BluetoothUtils";

    /** This utility class cannot be instantiated */
    private BluetoothUtils() {}

    /** Match with UserHandl.NULL but accessible inside bluetooth package */
    public static final UserHandle USER_HANDLE_NULL = UserHandle.of(-10000);

    /** Class for Length-Value-Entry array parsing */
    public static class TypeValueEntry {
        private final int mType;
        private final byte[] mValue;

        TypeValueEntry(int type, byte[] value) {
            mType = type;
            mValue = value;
        }

        @RequiresNoPermission
        public int getType() {
            return mType;
        }

        @RequiresNoPermission
        public byte[] getValue() {
            return mValue;
        }
    }

    // Helper method to extract bytes from byte array.
    private static byte[] extractBytes(byte[] rawBytes, int start, int length) {
        int remainingLength = rawBytes.length - start;
        if (remainingLength < length) {
            Log.w(
                    TAG,
                    "extractBytes() remaining length "
                            + remainingLength
                            + " is less than copying length "
                            + length
                            + ", array length is "
                            + rawBytes.length
                            + " start is "
                            + start);
            return null;
        }
        byte[] bytes = new byte[length];
        System.arraycopy(rawBytes, start, bytes, 0, length);
        return bytes;
    }

    /**
     * Parse Length Value Entry from raw bytes
     *
     * <p>The format is defined in Bluetooth 4.1 specification, Volume 3, Part C, Section 11 and 18.
     *
     * @param rawBytes raw bytes of Length-Value-Entry array
     * @hide
     */
    @SuppressWarnings("MixedMutabilityReturnType") // TODO(b/314811467)
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
                Log.w(
                        TAG,
                        "parseLtv() no type and value after length, rawBytes length = "
                                + rawBytes.length
                                + ", currentPost = "
                                + currentPos);
                break;
            }
            // Note the length includes the length of the field type itself.
            int dataLength = length - 1;
            // fieldType is unsigned int.
            int type = rawBytes[currentPos] & 0xFF;
            currentPos++;
            if (currentPos >= rawBytes.length) {
                Log.w(
                        TAG,
                        "parseLtv() no value after length, rawBytes length = "
                                + rawBytes.length
                                + ", currentPost = "
                                + currentPos);
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
     *
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
                Log.w(
                        TAG,
                        "serializeTypeValue() type "
                                + entry.getType()
                                + " is out of range of 0-0xFF");
                return null;
            }
            if (entry.getValue() == null) {
                Log.w(TAG, "serializeTypeValue() value is null");
                return null;
            }
            int lengthValue = entry.getValue().length + 1;
            if ((lengthValue - (lengthValue & 0xFF)) != 0) {
                Log.w(
                        TAG,
                        "serializeTypeValue() entry length "
                                + entry.getValue().length
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
     *
     * @param address Mac address to be log
     * @return Loggable mac address
     */
    public static String toAnonymizedAddress(String address) {
        if (address == null || address.length() != 17) {
            return null;
        }
        return "XX:XX:XX:XX" + address.substring(11);
    }

    /**
     * Simple alternative to {@link String#format} which purposefully supports only a small handful
     * of substitutions to improve execution speed. Benchmarking reveals this optimized alternative
     * performs 6.5x faster for a typical format string.
     *
     * <p>Below is a summary of the limited grammar supported by this method; if you need advanced
     * features, please continue using {@link String#format}.
     *
     * <ul>
     *   <li>{@code %b} for {@code boolean}
     *   <li>{@code %c} for {@code char}
     *   <li>{@code %d} for {@code int} or {@code long}
     *   <li>{@code %f} for {@code float} or {@code double}
     *   <li>{@code %s} for {@code String}
     *   <li>{@code %x} for hex representation of {@code int} or {@code long} or {@code byte}
     *   <li>{@code %%} for literal {@code %}
     *   <li>{@code %04d} style grammar to specify the argument width, such as {@code %04d} to
     *       prefix an {@code int} with zeros or {@code %10b} to prefix a {@code boolean} with
     *       spaces
     * </ul>
     *
     * <p>(copied from framework/base/core/java/android/text/TextUtils.java)
     *
     * <p>See {@code android.text.TextUtils.formatSimple}
     *
     * @throws IllegalArgumentException if the format string or arguments don't match the supported
     *     grammar described above.
     * @hide
     */
    public static @NonNull String formatSimple(@NonNull String format, Object... args) {
        final StringBuilder sb = new StringBuilder(format);
        int j = 0;
        for (int i = 0; i < sb.length(); ) {
            if (sb.charAt(i) == '%') {
                char code = sb.charAt(i + 1);

                // Decode any argument width request
                char prefixChar = '\0';
                int prefixLen = 0;
                int consume = 2;
                while ('0' <= code && code <= '9') {
                    if (prefixChar == '\0') {
                        prefixChar = (code == '0') ? '0' : ' ';
                    }
                    prefixLen *= 10;
                    prefixLen += Character.digit(code, 10);
                    consume += 1;
                    code = sb.charAt(i + consume - 1);
                }

                final String repl;
                switch (code) {
                    case 'b' -> {
                        if (j == args.length) {
                            throw new IllegalArgumentException("Too few arguments");
                        }
                        final Object arg = args[j++];
                        if (arg instanceof Boolean) {
                            repl = Boolean.toString((boolean) arg);
                        } else {
                            repl = Boolean.toString(arg != null);
                        }
                    }
                    case 'c', 'd', 'f', 's' -> {
                        if (j == args.length) {
                            throw new IllegalArgumentException("Too few arguments");
                        }
                        final Object arg = args[j++];
                        repl = String.valueOf(arg);
                    }
                    case 'x' -> {
                        if (j == args.length) {
                            throw new IllegalArgumentException("Too few arguments");
                        }
                        final Object arg = args[j++];
                        if (arg instanceof Integer) {
                            repl = Integer.toHexString((int) arg);
                        } else if (arg instanceof Long) {
                            repl = Long.toHexString((long) arg);
                        } else if (arg instanceof Byte) {
                            repl = Integer.toHexString(Byte.toUnsignedInt((byte) arg));
                        } else {
                            throw new IllegalArgumentException(
                                    "Unsupported hex type " + arg.getClass());
                        }
                    }
                    case '%' -> {
                        repl = "%";
                    }
                    default -> {
                        throw new IllegalArgumentException("Unsupported format code " + code);
                    }
                }

                sb.replace(i, i + consume, repl);

                // Apply any argument width request
                final int prefixInsert = (prefixChar == '0' && repl.charAt(0) == '-') ? 1 : 0;
                for (int k = repl.length(); k < prefixLen; k++) {
                    sb.insert(i + prefixInsert, prefixChar);
                }
                i += Math.max(repl.length(), prefixLen);
            } else {
                i++;
            }
        }
        if (j != args.length) {
            throw new IllegalArgumentException("Too many arguments");
        }
        return sb.toString();
    }

    /**
     * Wrapper for Parcel.writeString that silence AndroidFrameworkEfficientParcelable
     *
     * <p>ErrorProne wants us to use writeString8 but it is not exposed outside of fwk/base. The
     * alternative to deactivate entirely AndroidFrameworkEfficientParcelable is not good because
     * there are other error reported by it
     *
     * @hide
     */
    public static void writeStringToParcel(@NonNull Parcel out, @Nullable String str) {
        out.writeString(str);
    }

    /**
     * Execute the callback without UID / PID information
     *
     * @hide
     */
    public static void executeFromBinder(@NonNull Executor executor, @NonNull Runnable callback) {
        final long identity = Binder.clearCallingIdentity();
        try {
            executor.execute(() -> callback.run());
        } finally {
            Binder.restoreCallingIdentity(identity);
        }
    }

    /** A {@link Consumer} that automatically logs {@link RemoteException} @hide */
    @FunctionalInterface
    public interface RemoteExceptionIgnoringConsumer<T> {
        /** Called by {@code accept}. */
        void acceptOrThrow(T t) throws RemoteException;

        @RequiresNoPermission
        default void accept(T t) {
            try {
                acceptOrThrow(t);
            } catch (RemoteException ex) {
                logRemoteException(TAG, ex);
            }
        }
    }

    /** A {@link Function} that automatically logs {@link RemoteException} @hide */
    @FunctionalInterface
    public interface RemoteExceptionIgnoringFunction<T, R> {
        R applyOrThrow(T t) throws RemoteException;

        @RequiresNoPermission
        default R apply(T t, R defaultValue) {
            try {
                return applyOrThrow(t);
            } catch (RemoteException ex) {
                logRemoteException(TAG, ex);
                return defaultValue;
            }
        }
    }

    public static <S, R> R callService(
            S service, RemoteExceptionIgnoringFunction<S, R> function, R defaultValue) {
        return function.apply(service, defaultValue);
    }

    public static <S, R> R callServiceIfEnabled(
            BluetoothAdapter adapter,
            Supplier<S> provider,
            RemoteExceptionIgnoringFunction<S, R> function,
            R defaultValue) {
        if (!adapter.isEnabled()) {
            Log.d(TAG, "BluetoothAdapter is not enabled");
            return defaultValue;
        }
        final S service = provider.get();
        if (service == null) {
            Log.d(TAG, "Proxy not attached to service");
            return defaultValue;
        }
        return callService(service, function, defaultValue);
    }

    public static <S> void callServiceIfEnabled(
            BluetoothAdapter adapter,
            Supplier<S> provider,
            RemoteExceptionIgnoringConsumer<S> consumer) {
        if (!adapter.isEnabled()) {
            Log.d(TAG, "BluetoothAdapter is not enabled");
            return;
        }
        final S service = provider.get();
        if (service == null) {
            Log.d(TAG, "Proxy not attached to service");
            return;
        }
        consumer.accept(service);
    }

    /** return the current stack trace as a string without new line @hide */
    public static String inlineStackTrace() {
        StringBuilder sb = new StringBuilder();
        Arrays.stream(new Throwable().getStackTrace())
                .skip(1) // skip the inlineStackTrace method in the outputted stack trace
                .forEach(trace -> sb.append(" [at ").append(trace).append("]"));
        return sb.toString();
    }

    /** Gracefully print a RemoteException as a one line warning @hide */
    public static void logRemoteException(String tag, RemoteException ex) {
        Log.w(tag, ex.toString() + ": " + inlineStackTrace());
    }
}
