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

package com.android.clockwork.displayoffload;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.ArrayMap;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;

public class SystemFontHelper {
    // Path -> mapped ByteBuffer mapping
    private final Map<String, ByteBuffer> mStringByteBufferMap = new ArrayMap<>();
    // Resource id -> TTC Font Index mapping
    private final Map<Integer, Integer> mTtcFontIndexMap = new ArrayMap<>();

    public @Nullable ByteBuffer mapFont(String fullPath) {
        if (isMapped(fullPath)) {
            return mStringByteBufferMap.get(fullPath);
        }
        ByteBuffer buffer = mmap(fullPath);
        if (buffer == null) {
            return null;
        }
        mStringByteBufferMap.put(fullPath, buffer);
        return mStringByteBufferMap.get(fullPath);
    }

    public void setFontIndexForResourceId(int resourceId, int ttcIndex) {
        mTtcFontIndexMap.put(resourceId, ttcIndex);
    }

    public int getFontIndexForResourceId(int resourceId) {
        return mTtcFontIndexMap.getOrDefault(resourceId, 0);
    }

    public boolean isMapped(String fullPath) {
        return mStringByteBufferMap.containsKey(fullPath);
    }

    public void clear() {
        mTtcFontIndexMap.clear();
        mStringByteBufferMap.clear();
    }

    private static @Nullable ByteBuffer mmap(@NonNull String fullPath) {
        try (FileInputStream file = new FileInputStream(fullPath)) {
            final FileChannel fileChannel = file.getChannel();
            final long fontSize = fileChannel.size();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fontSize);
        } catch (IOException e) {
            return null;
        }
    }
}
