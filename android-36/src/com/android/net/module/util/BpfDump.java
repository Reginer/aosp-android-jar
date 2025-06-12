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
package com.android.net.module.util;

import static android.system.OsConstants.R_OK;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.BiFunction;

/**
 * The classes and the methods for BPF dump utilization.
 */
public class BpfDump {
    // Using "," as a separator between base64 encoded key and value is safe because base64
    // characters are [0-9a-zA-Z/=+].
    private static final String BASE64_DELIMITER = ",";

    /**
     * Encode BPF key and value into a base64 format string which uses the delimiter ',':
     * <base64 encoded key>,<base64 encoded value>
     */
    public static <K extends Struct, V extends Struct> String toBase64EncodedString(
            @NonNull final K key, @NonNull final V value) {
        final byte[] keyBytes = key.writeToBytes();
        final String keyBase64Str = Base64.encodeToString(keyBytes, Base64.DEFAULT)
                .replace("\n", "");
        final byte[] valueBytes = value.writeToBytes();
        final String valueBase64Str = Base64.encodeToString(valueBytes, Base64.DEFAULT)
                .replace("\n", "");

        return keyBase64Str + BASE64_DELIMITER + valueBase64Str;
    }

    /**
     * Decode Struct from a base64 format string
     */
    private static <T extends Struct> T parseStruct(
            Class<T> structClass, @NonNull String base64Str) {
        final byte[] bytes = Base64.decode(base64Str, Base64.DEFAULT);
        final ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.nativeOrder());
        return Struct.parse(structClass, byteBuffer);
    }

    /**
     * Decode BPF key and value from a base64 format string which uses the delimiter ',':
     * <base64 encoded key>,<base64 encoded value>
     */
    public static <K extends Struct, V extends Struct> Pair<K, V> fromBase64EncodedString(
            Class<K> keyClass, Class<V> valueClass, @NonNull String base64Str) {
        String[] keyValueStrs = base64Str.split(BASE64_DELIMITER);
        if (keyValueStrs.length != 2 /* key + value */) {
            throw new IllegalArgumentException("Invalid base64Str (" + base64Str + "), base64Str"
                    + " must contain exactly one delimiter '" + BASE64_DELIMITER + "'");
        }
        final K k = parseStruct(keyClass, keyValueStrs[0]);
        final V v = parseStruct(valueClass, keyValueStrs[1]);
        return new Pair<>(k, v);
    }

    /**
     * Dump the BpfMap entries with base64 format encoding.
     */
    public static <K extends Struct, V extends Struct> void dumpRawMap(IBpfMap<K, V> map,
            PrintWriter pw) {
        try {
            if (map == null) {
                pw.println("BPF map is null");
                return;
            }
            if (map.isEmpty()) {
                pw.println("No entries");
                return;
            }
            map.forEach((k, v) -> pw.println(toBase64EncodedString(k, v)));
        } catch (ErrnoException e) {
            pw.println("Map dump end with error: " + Os.strerror(e.errno));
        }
    }

    /**
     * Dump the BpfMap name and entries
     */
    public static <K extends Struct, V extends Struct> void dumpMap(IBpfMap<K, V> map,
            PrintWriter pw, String mapName, BiFunction<K, V, String> entryToString) {
        dumpMap(map, pw, mapName, "" /* header */, entryToString);
    }

    /**
     * Dump the BpfMap name, header, and entries
     */
    public static <K extends Struct, V extends Struct> void dumpMap(IBpfMap<K, V> map,
            PrintWriter pw, String mapName, String header, BiFunction<K, V, String> entryToString) {
        pw.println(mapName + ":");
        if (!header.isEmpty()) {
            pw.println("  " + header);
        }
        try {
            map.forEach((key, value) -> {
                // Value could be null if there is a concurrent entry deletion.
                // http://b/220084230.
                if (value != null) {
                    pw.println("  " + entryToString.apply(key, value));
                } else {
                    pw.println("Entry is deleted while dumping, iterating from first entry");
                }
            });
        } catch (ErrnoException e) {
            pw.println("Map dump end with error: " + Os.strerror(e.errno));
        }
    }

    /**
     * Dump the BpfMap status
     */
    public static <K extends Struct, V extends Struct> void dumpMapStatus(IBpfMap<K, V> map,
            PrintWriter pw, String mapName, String path) {
        if (map != null) {
            pw.println(mapName + ": OK");
            return;
        }
        try {
            Os.access(path, R_OK);
            pw.println(mapName + ": NULL(map is pinned to " + path + ")");
        } catch (ErrnoException e) {
            pw.println(mapName + ": NULL(map is not pinned to " + path + ": "
                    + Os.strerror(e.errno) + ")");
        }
    }

    // TODO: add a helper to dump bpf map content with the map name, the header line
    // (ex: "BPF ingress map: iif nat64Prefix v6Addr -> v4Addr oif"), a lambda that
    // knows how to dump each line, and the PrintWriter.
}
