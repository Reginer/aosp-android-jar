/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static android.system.OsConstants.EBUSY;
import static android.system.OsConstants.EEXIST;
import static android.system.OsConstants.ENOENT;

import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BpfMap is a key -> value mapping structure that is designed to maintained the bpf map entries.
 * This is a wrapper class of in-kernel data structure. The in-kernel data can be read/written by
 * passing syscalls with map file descriptor.
 *
 * @param <K> the key of the map.
 * @param <V> the value of the map.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class BpfMap<K extends Struct, V extends Struct> implements IBpfMap<K, V> {
    static {
        System.loadLibrary(JniUtil.getJniLibraryName(BpfMap.class.getPackage()));
    }

    // Following definitions from kernel include/uapi/linux/bpf.h
    public static final int BPF_F_RDWR = 0;
    public static final int BPF_F_RDONLY = 1 << 3;
    public static final int BPF_F_WRONLY = 1 << 4;

    // magic value for jni consumption, invalid from kernel point of view
    public static final int BPF_F_RDWR_EXCLUSIVE = BPF_F_RDONLY | BPF_F_WRONLY;

    public static final int BPF_MAP_TYPE_HASH = 1;

    private static final int BPF_F_NO_PREALLOC = 1;

    private static final int BPF_ANY = 0;
    private static final int BPF_NOEXIST = 1;
    private static final int BPF_EXIST = 2;

    private final ParcelFileDescriptor mMapFd;
    private final Class<K> mKeyClass;
    private final Class<V> mValueClass;
    private final int mKeySize;
    private final int mValueSize;

    private static ConcurrentHashMap<Pair<String, Integer>, ParcelFileDescriptor> sFdCache =
            new ConcurrentHashMap<>();

    private static ParcelFileDescriptor checkModeExclusivity(ParcelFileDescriptor fd, int mode)
            throws ErrnoException {
        if (mode == BPF_F_RDWR_EXCLUSIVE) throw new ErrnoException("cachedBpfFdGet", EBUSY);
        return fd;
    }

    private static ParcelFileDescriptor cachedBpfFdGet(String path, int mode,
                                                       int keySize, int valueSize)
            throws ErrnoException, NullPointerException {
        // Supports up to 1023 byte key and 65535 byte values
        // Creating a BpfMap with larger keys/values seems like a bad idea any way...
        keySize &= 1023; // 10-bits
        valueSize &= 65535; // 16-bits
        var key = Pair.create(path, (mode << 26) ^ (keySize << 16) ^ valueSize);
        // unlocked fetch is safe: map is concurrent read capable, and only inserted into
        ParcelFileDescriptor fd = sFdCache.get(key);
        if (fd != null) return checkModeExclusivity(fd, mode);
        // ok, no cached fd present, need to grab a lock
        synchronized (BpfMap.class) {
            // need to redo the check
            fd = sFdCache.get(key);
            if (fd != null) return checkModeExclusivity(fd, mode);
            // okay, we really haven't opened this before...
            fd = ParcelFileDescriptor.adoptFd(nativeBpfFdGet(path, mode, keySize, valueSize));
            sFdCache.put(key, fd);
            return fd;
        }
    }

    /**
     * Create a BpfMap map wrapper with "path" of filesystem.
     *
     * @param flag the access mode, one of BPF_F_RDWR, BPF_F_RDONLY, or BPF_F_WRONLY.
     * @throws ErrnoException if the BPF map associated with {@code path} cannot be retrieved.
     * @throws NullPointerException if {@code path} is null.
     */
    public BpfMap(@NonNull final String path, final int flag, final Class<K> key,
            final Class<V> value) throws ErrnoException, NullPointerException {
        mKeyClass = key;
        mValueClass = value;
        mKeySize = Struct.getSize(key);
        mValueSize = Struct.getSize(value);
        mMapFd = cachedBpfFdGet(path, flag, mKeySize, mValueSize);
    }

    /**
     * Create a R/W BpfMap map wrapper with "path" of filesystem.
     *
     * @throws ErrnoException if the BPF map associated with {@code path} cannot be retrieved.
     * @throws NullPointerException if {@code path} is null.
     */
    public BpfMap(@NonNull final String path, final Class<K> key,
            final Class<V> value) throws ErrnoException, NullPointerException {
        this(path, BPF_F_RDWR, key, value);
    }

    /**
     * Update an existing or create a new key -> value entry in an eBbpf map.
     * (use insertOrReplaceEntry() if you need to know whether insert or replace happened)
     */
    @Override
    public void updateEntry(K key, V value) throws ErrnoException {
        nativeWriteToMapEntry(mMapFd.getFd(), key.writeToBytes(), value.writeToBytes(), BPF_ANY);
    }

    /**
     * If the key does not exist in the map, insert key -> value entry into eBpf map.
     * Otherwise IllegalStateException will be thrown.
     */
    @Override
    public void insertEntry(K key, V value)
            throws ErrnoException, IllegalStateException {
        try {
            nativeWriteToMapEntry(mMapFd.getFd(), key.writeToBytes(), value.writeToBytes(),
                    BPF_NOEXIST);
        } catch (ErrnoException e) {
            if (e.errno == EEXIST) throw new IllegalStateException(key + " already exists");

            throw e;
        }
    }

    /**
     * If the key already exists in the map, replace its value. Otherwise NoSuchElementException
     * will be thrown.
     */
    @Override
    public void replaceEntry(K key, V value)
            throws ErrnoException, NoSuchElementException {
        try {
            nativeWriteToMapEntry(mMapFd.getFd(), key.writeToBytes(), value.writeToBytes(),
                    BPF_EXIST);
        } catch (ErrnoException e) {
            if (e.errno == ENOENT) throw new NoSuchElementException(key + " not found");

            throw e;
        }
    }

    /**
     * Update an existing or create a new key -> value entry in an eBbpf map.
     * Returns true if inserted, false if replaced.
     * (use updateEntry() if you don't care whether insert or replace happened)
     * Note: see inline comment below if running concurrently with delete operations.
     */
    @Override
    public boolean insertOrReplaceEntry(K key, V value)
            throws ErrnoException {
        try {
            nativeWriteToMapEntry(mMapFd.getFd(), key.writeToBytes(), value.writeToBytes(),
                    BPF_NOEXIST);
            return true;   /* insert succeeded */
        } catch (ErrnoException e) {
            if (e.errno != EEXIST) throw e;
        }
        try {
            nativeWriteToMapEntry(mMapFd.getFd(), key.writeToBytes(), value.writeToBytes(),
                    BPF_EXIST);
            return false;   /* replace succeeded */
        } catch (ErrnoException e) {
            if (e.errno != ENOENT) throw e;
        }
        /* If we reach here somebody deleted after our insert attempt and before our replace:
         * this implies a race happened.  The kernel bpf delete interface only takes a key,
         * and not the value, so we can safely pretend the replace actually succeeded and
         * was immediately followed by the other thread's delete, since the delete cannot
         * observe the potential change to the value.
         */
        return false;   /* pretend replace succeeded */
    }

    /** Remove existing key from eBpf map. Return false if map was not modified. */
    @Override
    public boolean deleteEntry(K key) throws ErrnoException {
        return nativeDeleteMapEntry(mMapFd.getFd(), key.writeToBytes());
    }

    private K getNextKeyInternal(@Nullable K key) throws ErrnoException {
        byte[] rawKey = new byte[mKeySize];

        if (!nativeGetNextMapKey(mMapFd.getFd(),
                                 key == null ? null : key.writeToBytes(),
                                 rawKey)) return null;

        final ByteBuffer buffer = ByteBuffer.wrap(rawKey);
        buffer.order(ByteOrder.nativeOrder());
        return Struct.parse(mKeyClass, buffer);
    }

    /**
     * Get the next key of the passed-in key. If the passed-in key is not found, return the first
     * key. If the passed-in key is the last one, return null.
     *
     * TODO: consider allowing null passed-in key.
     */
    @Override
    public K getNextKey(@NonNull K key) throws ErrnoException {
        Objects.requireNonNull(key);
        return getNextKeyInternal(key);
    }

    /** Get the first key of eBpf map. */
    @Override
    public K getFirstKey() throws ErrnoException {
        return getNextKeyInternal(null);
    }

    /** Check whether a key exists in the map. */
    @Override
    public boolean containsKey(@NonNull K key) throws ErrnoException {
        Objects.requireNonNull(key);

        byte[] rawValue = new byte[mValueSize];
        return nativeFindMapEntry(mMapFd.getFd(), key.writeToBytes(), rawValue);
    }

    /** Retrieve a value from the map. Return null if there is no such key. */
    @Override
    public V getValue(@NonNull K key) throws ErrnoException {
        Objects.requireNonNull(key);

        byte[] rawValue = new byte[mValueSize];
        if (!nativeFindMapEntry(mMapFd.getFd(), key.writeToBytes(), rawValue)) return null;

        final ByteBuffer buffer = ByteBuffer.wrap(rawValue);
        buffer.order(ByteOrder.nativeOrder());
        return Struct.parse(mValueClass, buffer);
    }

    /** Synchronize Kernel RCU */
    public static void synchronizeKernelRCU() throws ErrnoException {
        nativeSynchronizeKernelRCU();
    }

    private static native int nativeBpfFdGet(String path, int mode, int keySize, int valueSize)
            throws ErrnoException, NullPointerException;

    // Note: the following methods appear to not require the object by virtue of taking the
    // fd as an int argument, but the hidden reference to this is actually what prevents
    // the object from being garbage collected (and thus potentially maps closed) prior
    // to the native code actually running (with a possibly already closed fd).

    private native void nativeWriteToMapEntry(int fd, byte[] key, byte[] value, int flags)
            throws ErrnoException;

    private native boolean nativeDeleteMapEntry(int fd, byte[] key) throws ErrnoException;

    // If key is found, the operation returns true and the nextKey would reference to the next
    // element.  If key is not found, the operation returns true and the nextKey would reference to
    // the first element.  If key is the last element, false is returned.
    private native boolean nativeGetNextMapKey(int fd, byte[] key, byte[] nextKey)
            throws ErrnoException;

    private native boolean nativeFindMapEntry(int fd, byte[] key, byte[] value)
            throws ErrnoException;

    private static native void nativeSynchronizeKernelRCU() throws ErrnoException;
}
