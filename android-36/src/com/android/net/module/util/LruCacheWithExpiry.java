/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.util.LruCache;

import com.android.internal.annotations.GuardedBy;

import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A thread-safe LRU cache that stores key-value pairs with an expiry time.
 *
 * <p>This cache uses an {@link LruCache} to store entries and evicts the least
 * recently used entries when the cache reaches its maximum capacity. It also
 * supports an expiry time for each entry, allowing entries to be automatically
 * removed from the cache after a certain duration.
 *
 * @param <K> The type of keys used to identify cached entries.
 * @param <V> The type of values stored in the cache.
 *
 * @hide
 */
public class LruCacheWithExpiry<K, V> {
    private final LongSupplier mTimeSupplier;
    private final long mExpiryDurationMs;
    @GuardedBy("mMap")
    private final LruCache<K, CacheValue<V>> mMap;
    private final Predicate<V> mShouldCacheValue;

    /**
     * Constructs a new {@link LruCacheWithExpiry} with the specified parameters.
     *
     * @param timeSupplier     The {@link java.util.function.LongSupplier} to use for
     *                         determining timestamps.
     * @param expiryDurationMs The expiry duration for cached entries in milliseconds.
     * @param maxSize          The maximum number of entries to hold in the cache.
     * @param shouldCacheValue A {@link Predicate} that determines whether a given value should be
     *                         cached. This can be used to filter out certain values from being
     *                         stored in the cache.
     */
    public LruCacheWithExpiry(@NonNull LongSupplier timeSupplier, long expiryDurationMs,
            int maxSize, Predicate<V> shouldCacheValue) {
        mTimeSupplier = timeSupplier;
        mExpiryDurationMs = expiryDurationMs;
        mMap = new LruCache<>(maxSize);
        mShouldCacheValue = shouldCacheValue;
    }

    /**
     * Retrieves a value from the cache, associated with the given key.
     *
     * @param key The key to look up in the cache.
     * @return The cached value, or {@code null} if not found or expired.
     */
    @Nullable
    public V get(@NonNull K key) {
        synchronized (mMap) {
            final CacheValue<V> value = mMap.get(key);
            if (value != null && !isExpired(value.timestamp)) {
                return value.entry;
            } else {
                mMap.remove(key); // Remove expired entries
                return null;
            }
        }
    }

    /**
     * Retrieves a value from the cache, associated with the given key.
     * If the entry is not found in the cache or has expired, computes it using the provided
     * {@code supplier} and stores the result in the cache.
     *
     * @param key      The key to look up in the cache.
     * @param supplier The {@link Supplier} to compute the value if not found or expired.
     * @return The cached or computed value, or {@code null} if the {@code supplier} returns null.
     */
    @Nullable
    public V getOrCompute(@NonNull K key, @NonNull Supplier<V> supplier) {
        synchronized (mMap) {
            final V cachedValue = get(key);
            if (cachedValue != null) {
                return cachedValue;
            }

            // Entry not found or expired, compute it
            final V computedValue = supplier.get();
            if (computedValue != null && mShouldCacheValue.test(computedValue)) {
                put(key, computedValue);
            }
            return computedValue;
        }
    }

    /**
     * Stores a value in the cache, associated with the given key.
     *
     * @param key   The key to associate with the value.
     * @param value The value to store in the cache.
     */
    public void put(@NonNull K key, @NonNull V value) {
        Objects.requireNonNull(value);
        synchronized (mMap) {
            mMap.put(key, new CacheValue<>(mTimeSupplier.getAsLong(), value));
        }
    }

    /**
     * Stores a value in the cache if absent, associated with the given key.
     *
     * @param key   The key to associate with the value.
     * @param value The value to store in the cache.
     * @return The existing value associated with the key, if present; otherwise, null.
     */
    @Nullable
    public V putIfAbsent(@NonNull K key, @NonNull V value) {
        Objects.requireNonNull(value);
        synchronized (mMap) {
            final V existingValue = get(key);
            if (existingValue == null) {
                put(key, value);
            }
            return existingValue;
        }
    }

    /**
     * Clear the cache.
     */
    public void clear() {
        synchronized (mMap) {
            mMap.evictAll();
        }
    }

    private boolean isExpired(long timestamp) {
        return mTimeSupplier.getAsLong() > timestamp + mExpiryDurationMs;
    }

    private static class CacheValue<V> {
        public final long timestamp;
        @NonNull
        public final V entry;

        CacheValue(long timestamp, V entry) {
            this.timestamp = timestamp;
            this.entry = entry;
        }
    }
}
