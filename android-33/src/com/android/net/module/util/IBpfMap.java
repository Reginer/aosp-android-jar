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

import java.util.NoSuchElementException;
import java.util.function.BiConsumer;

/**
 * The interface of BpfMap. This could be used to inject for testing.
 * So the testing code won't load the JNI and update the entries to kernel.
 *
 * @param <K> the key of the map.
 * @param <V> the value of the map.
 */
public interface IBpfMap<K extends Struct, V extends Struct> {
    /** Update an existing or create a new key -> value entry in an eBbpf map. */
    void updateEntry(K key, V value) throws ErrnoException;

    /** If the key does not exist in the map, insert key -> value entry into eBpf map. */
    void insertEntry(K key, V value) throws ErrnoException, IllegalStateException;

    /** If the key already exists in the map, replace its value. */
    void replaceEntry(K key, V value) throws ErrnoException, NoSuchElementException;

    /**
     * Update an existing or create a new key -> value entry in an eBbpf map. Returns true if
     * inserted, false if replaced. (use updateEntry() if you don't care whether insert or replace
     * happened).
     */
    boolean insertOrReplaceEntry(K key, V value) throws ErrnoException;

    /** Remove existing key from eBpf map. Return true if something was deleted. */
    boolean deleteEntry(K key) throws ErrnoException;

    /** Returns {@code true} if this map contains no elements. */
    boolean isEmpty() throws ErrnoException;

    /** Get the key after the passed-in key. */
    K getNextKey(@NonNull K key) throws ErrnoException;

    /** Get the first key of the eBpf map. */
    K getFirstKey() throws ErrnoException;

    /** Check whether a key exists in the map. */
    boolean containsKey(@NonNull K key) throws ErrnoException;

    /** Retrieve a value from the map. */
    V getValue(@NonNull K key) throws ErrnoException;

    public interface ThrowingBiConsumer<T,U> {
        void accept(T t, U u) throws ErrnoException;
    }

    /**
     * Iterate through the map and handle each key -> value retrieved base on the given BiConsumer.
     */
    void forEach(ThrowingBiConsumer<K, V> action) throws ErrnoException;

    /** Clears the map. */
    void clear() throws ErrnoException;
}
