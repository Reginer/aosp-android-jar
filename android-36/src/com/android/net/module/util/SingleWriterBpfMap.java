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

import android.os.Build;
import android.system.ErrnoException;
import android.util.Pair;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * Subclass of BpfMap for maps that are only ever written by one userspace writer.
 *
 * This class stores all map data in a userspace HashMap in addition to in the BPF map. This makes
 * reads (but not iterations) much faster because they do not require a system call or converting
 * the raw map read to the Value struct. See, e.g., b/343166906 .
 *
 * Users of this class must ensure that no BPF program ever writes to the map, and that all
 * userspace writes to the map occur through this object. Other userspace code may still read from
 * the map; only writes are required to go through this object.
 *
 * Reads and writes to this object are thread-safe and internally synchronized. The read and write
 * methods are synchronized to ensure that current writers always result in a consistent internal
 * state (without synchronization, two concurrent writes might update the underlying map and the
 * cache in the opposite order, resulting in the cache being out of sync with the map).
 *
 * getNextKey and iteration over the map are not synchronized or cached and always access the
 * isunderlying map. The values returned by these calls may be temporarily out of sync with the
 * values read and written through this object.
 *
 * TODO: consider caching reads on iterations as well. This is not trivial because the semantics for
 * iterating BPF maps require passing in the previously-returned key, and Java iterators only
 * support iterating from the beginning. It could be done by implementing forEach and possibly by
 * making getFirstKey and getNextKey private (if no callers are using them). Because HashMap is not
 * thread-safe, implementing forEach would require either making that method synchronized (and
 * block reads and updates from other threads until iteration is complete) or switching the
 * internal HashMap to ConcurrentHashMap.
 *
 * @param <K> the key of the map.
 * @param <V> the value of the map.
 */
@RequiresApi(Build.VERSION_CODES.S)
public class SingleWriterBpfMap<K extends Struct, V extends Struct> extends BpfMap<K, V> {
    // HashMap instead of ArrayMap because it performs better on larger maps, and many maps used in
    // our code can contain hundreds of items.
    @GuardedBy("this")
    private final HashMap<K, V> mCache = new HashMap<>();

    // This should only ever be called (hence private) once for a given 'path'.
    // Java-wise what matters is the entire {path, key, value} triplet,
    // but of course the kernel exclusive lock is just on the path (fd),
    // and any BpfMap has (or should have...) well defined key/value types
    // (or at least their sizes) so in practice it doesn't really matter.
    private SingleWriterBpfMap(@NonNull final String path, final Class<K> key,
            final Class<V> value) throws ErrnoException, NullPointerException {
        super(path, BPF_F_RDWR_EXCLUSIVE, key, value);

        // Populate cache with the current map contents.
        synchronized (this) {
            K currentKey = super.getFirstKey();
            while (currentKey != null) {
                mCache.put(currentKey, super.getValue(currentKey));
                currentKey = super.getNextKey(currentKey);
            }
        }
    }

    // This allows reuse of SingleWriterBpfMap objects for the same {path, keyClass, valueClass}.
    // These are never destroyed, so once created the lock is (effectively) held till process death
    // (even if fixed, there would still be a write-only fd cache in underlying BpfMap base class).
    private static final HashMap<Pair<String, Pair<Class, Class>>, SingleWriterBpfMap>
            singletonCache = new HashMap<>();

    // This is the public 'factory method' that (creates if needed and) returns a singleton instance
    // for a given map.  This holds an exclusive lock and has a permanent write-through cache.
    // It will not be released until process death (or at least unload of the relevant class loader)
    public synchronized static <KK extends Struct, VV extends Struct> SingleWriterBpfMap<KK,VV>
            getSingleton(@NonNull final String path, final Class<KK> key, final Class<VV> value)
            throws ErrnoException, NullPointerException {
        var cacheKey = new Pair<>(path, new Pair<Class,Class>(key, value));
        if (!singletonCache.containsKey(cacheKey))
            singletonCache.put(cacheKey, new SingleWriterBpfMap(path, key, value));
        return singletonCache.get(cacheKey);
    }

    @Override
    public synchronized void updateEntry(K key, V value) throws ErrnoException {
        super.updateEntry(key, value);
        mCache.put(key, value);
    }

    @Override
    public synchronized void insertEntry(K key, V value)
            throws ErrnoException, IllegalStateException {
        super.insertEntry(key, value);
        mCache.put(key, value);
    }

    @Override
    public synchronized void replaceEntry(K key, V value)
            throws ErrnoException, NoSuchElementException {
        super.replaceEntry(key, value);
        mCache.put(key, value);
    }

    @Override
    public synchronized boolean insertOrReplaceEntry(K key, V value) throws ErrnoException {
        final boolean ret = super.insertOrReplaceEntry(key, value);
        mCache.put(key, value);
        return ret;
    }

    @Override
    public synchronized boolean deleteEntry(K key) throws ErrnoException {
        final boolean ret = super.deleteEntry(key);
        mCache.remove(key);
        return ret;
    }

    @Override
    public synchronized boolean containsKey(@NonNull K key) throws ErrnoException {
        return mCache.containsKey(key);
    }

    @Override
    public synchronized V getValue(@NonNull K key) throws ErrnoException {
        return mCache.get(key);
    }
}
