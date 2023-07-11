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

import android.system.ErrnoException;

import androidx.annotation.NonNull;

 /**
 *
 * Generic bitmap class for use with BPF programs. Corresponds to a BpfMap
 * array type with key->int and value->uint64_t defined in the bpf program.
 *
 */
public class BpfBitmap {
    private BpfMap<Struct.U32, Struct.S64> mBpfMap;

    /**
     * Create a BpfBitmap map wrapper with "path" of filesystem.
     *
     * @param path The path of the BPF map.
     */
    public BpfBitmap(@NonNull String path) throws ErrnoException {
        mBpfMap = new BpfMap<Struct.U32, Struct.S64>(path, BpfMap.BPF_F_RDWR,
                Struct.U32.class, Struct.S64.class);
    }

    /**
     * Retrieves the value from BpfMap for the given key.
     *
     * @param key The key in the map corresponding to the value to return.
     */
    private long getBpfMapValue(Struct.U32 key) throws ErrnoException  {
        Struct.S64 curVal = mBpfMap.getValue(key);
        if (curVal != null) {
            return curVal.val;
        } else {
            return 0;
        }
    }

    /**
     * Retrieves the bit for the given index in the bitmap.
     *
     * @param index Position in bitmap.
     */
    public boolean get(int index) throws ErrnoException  {
        if (index < 0) return false;

        Struct.U32 key = new Struct.U32(index >> 6);
        return ((getBpfMapValue(key) >>> (index & 63)) & 1L) != 0;
    }

    /**
     * Set the specified index in the bitmap.
     *
     * @param index Position to set in bitmap.
     */
    public void set(int index) throws ErrnoException {
        set(index, true);
    }

    /**
     * Unset the specified index in the bitmap.
     *
     * @param index Position to unset in bitmap.
     */
    public void unset(int index) throws ErrnoException {
        set(index, false);
    }

    /**
     * Change the specified index in the bitmap to set value.
     *
     * @param index Position to unset in bitmap.
     * @param set Boolean indicating to set or unset index.
     */
    public void set(int index, boolean set) throws ErrnoException {
        if (index < 0) throw new IllegalArgumentException("Index out of bounds.");

        Struct.U32 key = new Struct.U32(index >> 6);
        long mask = (1L << (index & 63));
        long val = getBpfMapValue(key);
        if (set) val |= mask; else val &= ~mask;
        mBpfMap.updateEntry(key, new Struct.S64(val));
    }

    /**
     * Clears the map. The map may already be empty.
     *
     * @throws ErrnoException if updating entry to 0 fails.
     */
    public void clear() throws ErrnoException {
        mBpfMap.forEach((key, value) -> {
            mBpfMap.updateEntry(key, new Struct.S64(0));
        });
    }

    /**
     * Checks if all bitmap values are 0.
     */
    public boolean isEmpty() throws ErrnoException {
        Struct.U32 key = mBpfMap.getFirstKey();
        while (key != null) {
            if (getBpfMapValue(key) != 0) {
                return false;
            }
            key = mBpfMap.getNextKey(key);
        }
        return true;
    }
}
