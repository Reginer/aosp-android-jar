/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.internal.telephony;

import android.annotation.NonNull;
import android.telephony.Rlog;

import com.android.internal.annotations.VisibleForTesting;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * Caches WAP push PDU data for retrieval during MMS downloading.
 * When on a satellite connection, the cached message size will be used to prevent downloading
 * messages that exceed a threshold.
 *
 * The cache uses a circular buffer and will start invalidating the oldest entries after 250
 * message sizes have been inserted.
 * The cache also invalidates entries that have been in the cache for over 14 days.
 */
public class WapPushCache {
    private static final String TAG = "WAP PUSH CACHE";

    // Because we store each size twice, this represents 250 messages. That limit is chosen so
    // that the memory footprint of the cache stays reasonably small while still supporting what
    // we guess will be the vast majority of real use cases.
    private static final int MAX_CACHE_SIZE = 500;

    // WAP push PDUs have an expiry property, but we can't be certain that it is set accurately
    // by the carrier. We will use our own expiry for this cache to keep it small. One example
    // carrier has an expiry of 7 days so 14 will give us room for those with longer times as well.
    private static final long CACHE_EXPIRY_TIME = TimeUnit.DAYS.toMillis(14);

    private static final HashMap<String, CacheEntry> sMessageSizes = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Entry<String, CacheEntry> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    @VisibleForTesting
    public static TelephonyFacade sTelephonyFacade = new TelephonyFacade();

    /**
     * Puts a WAP push PDU's messageSize in the cache.
     *
     * The data is stored twice, once using just locationUrl as the key and once
     * using transactionId appended to the locationUrl. For some carriers, xMS apps
     * append the transactionId to the location and we need to support lookup using either the
     * original location or one modified in this way.

     *
     * @param locationUrl location of the message used as part of the cache key.
     * @param transactionId message transaction ID used as part of the cache key.
     * @param messageSize size of the message to be stored in the cache.
     */
    public static void putWapMessageSize(
            @NonNull byte[] locationUrl,
            @NonNull byte[] transactionId,
            long messageSize
    ) {
        long expiry = sTelephonyFacade.getElapsedSinceBootMillis() + CACHE_EXPIRY_TIME;
        if (messageSize <= 0) {
            Rlog.e(TAG, "Invalid message size of " + messageSize + ". Not inserting.");
            return;
        }
        synchronized (sMessageSizes) {
            sMessageSizes.put(Arrays.toString(locationUrl), new CacheEntry(messageSize, expiry));

            // concatenate the locationUrl and transactionId
            byte[] joinedKey = ByteBuffer
                    .allocate(locationUrl.length + transactionId.length)
                    .put(locationUrl)
                    .put(transactionId)
                    .array();
            sMessageSizes.put(Arrays.toString(joinedKey), new CacheEntry(messageSize, expiry));
            invalidateOldEntries();
        }
    }

    /**
     * Remove entries from the cache that are older than CACHE_EXPIRY_TIME
     */
    private static void invalidateOldEntries() {
        long currentTime = sTelephonyFacade.getElapsedSinceBootMillis();

        // We can just remove elements from the start until one is found that does not exceed the
        // expiry since the elements are in order of insertion.
        for (Iterator<CacheEntry> it = sMessageSizes.values().iterator(); it.hasNext(); ) {
            CacheEntry entry = it.next();
            if (entry.mExpiry < currentTime) {
                it.remove();
            } else {
                break;
            }
        }
    }

    /**
     * Gets the message size of a WAP from the cache.
     *
     * Because we stored the size both using the location+transactionId key and using the
     * location only key, we should be able to find the size whether the xMS app modified
     * the location or not.
     *
     * @param locationUrl the location to use as a key for looking up the size in the cache.
     *
     * @return long representing the message size of the WAP
     * @throws NoSuchElementException if the WAP doesn't exist in the cache
     * @throws IllegalArgumentException if the locationUrl is empty
     */
    public static long getWapMessageSize(@NonNull byte[] locationUrl) {
        if (locationUrl.length == 0) {
            throw new IllegalArgumentException("Found empty locationUrl");
        }
        CacheEntry entry = sMessageSizes.get(Arrays.toString(locationUrl));
        if (entry == null) {
            throw new NoSuchElementException(
                "No cached WAP size for locationUrl " + Arrays.toString(locationUrl)
            );
        }
        return entry.mSize;
    }

    /**
     * Clears all elements from the cache
     */
    @VisibleForTesting
    public static void clear() {
        sMessageSizes.clear();
    }

    /**
     * Returns a count of the number of elements in the cache
     * @return count of elements
     */
    @VisibleForTesting
    public static int size() {
        return sMessageSizes.size();
    }



    private static class CacheEntry {
        CacheEntry(long size, long expiry) {
            mSize = size;
            mExpiry = expiry;
        }
        private final long mSize;
        private final long mExpiry;
    }
}
