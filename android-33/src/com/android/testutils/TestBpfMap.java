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

package com.android.testutils;

import android.system.ErrnoException;

import androidx.annotation.NonNull;

import com.android.net.module.util.BpfMap;
import com.android.net.module.util.IBpfMap.ThrowingBiConsumer;
import com.android.net.module.util.Struct;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * Fake BPF map class for tests that have no no privilege to access real BPF maps. All member
 * functions which eventually call JNI to access the real native BPF map are overridden.
 *
 * Inherits from BpfMap instead of implementing IBpfMap so that any class using a BpfMap can use
 * this class in its tests.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class TestBpfMap<K extends Struct, V extends Struct> extends BpfMap<K, V> {
    private final ConcurrentHashMap<K, V> mMap = new ConcurrentHashMap<>();

    public TestBpfMap(final Class<K> key, final Class<V> value) {
        super(key, value);
    }

    @Override
    public void forEach(ThrowingBiConsumer<K, V> action) throws ErrnoException {
        // TODO: consider using mocked #getFirstKey and #getNextKey to iterate. It helps to
        // implement the entry deletion in the iteration if required.
        for (Map.Entry<K, V> entry : mMap.entrySet()) {
            action.accept(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void updateEntry(K key, V value) throws ErrnoException {
        mMap.put(key, value);
    }

    @Override
    public void insertEntry(K key, V value) throws ErrnoException,
            IllegalArgumentException {
        // The entry is created if and only if it doesn't exist. See BpfMap#insertEntry.
        if (mMap.get(key) != null) {
            throw new IllegalArgumentException(key + " already exist");
        }
        mMap.put(key, value);
    }

    @Override
    public void replaceEntry(K key, V value) throws ErrnoException, NoSuchElementException {
        if (!mMap.containsKey(key)) throw new NoSuchElementException();
        mMap.put(key, value);
    }

    @Override
    public boolean insertOrReplaceEntry(K key, V value) throws ErrnoException {
        // Returns true if inserted, false if replaced.
        boolean ret = !mMap.containsKey(key);
        mMap.put(key, value);
        return ret;
    }

    @Override
    public boolean deleteEntry(Struct key) throws ErrnoException {
        return mMap.remove(key) != null;
    }

    @Override
    public boolean isEmpty() throws ErrnoException {
        return mMap.isEmpty();
    }

    @Override
    public K getNextKey(@NonNull K key) {
        // Expensive, but since this is only for tests...
        Iterator<K> it = mMap.keySet().iterator();
        while (it.hasNext()) {
            if (Objects.equals(it.next(), key)) {
                return it.hasNext() ? it.next() : null;
            }
        }
        return null;
    }

    @Override
    public K getFirstKey() {
        for (K key : mMap.keySet()) {
            return key;
        }
        return null;
    }

    @Override
    public boolean containsKey(@NonNull K key) throws ErrnoException {
        return mMap.containsKey(key);
    }

    @Override
    public V getValue(@NonNull K key) throws ErrnoException {
        // Return value for a given key. Otherwise, return null without an error ENOENT.
        // BpfMap#getValue treats that the entry is not found as no error.
        return mMap.get(key);
    }

    @Override
    public void clear() throws ErrnoException {
        // TODO: consider using mocked #getFirstKey and #deleteEntry to implement.
        mMap.clear();
    }
}
