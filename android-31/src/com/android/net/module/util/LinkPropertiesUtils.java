/*
 * Copyright (C) 2019 The Android Open Source Project
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
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.RouteInfo;
import android.text.TextUtils;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Collection of link properties utilities.
 * @hide
 */
public final class LinkPropertiesUtils {

    /**
     * @param <T> The type of data to compare.
     */
    public static class CompareResult<T> {
        public final List<T> removed = new ArrayList<>();
        public final List<T> added = new ArrayList<>();

        public CompareResult() {}

        public CompareResult(@Nullable Collection<T> oldItems, @Nullable Collection<T> newItems) {
            if (oldItems != null) {
                removed.addAll(oldItems);
            }
            if (newItems != null) {
                for (T newItem : newItems) {
                    if (!removed.remove(newItem)) {
                        added.add(newItem);
                    }
                }
            }
        }

        @Override
        public String toString() {
            return "removed=[" + TextUtils.join(",", removed)
                    + "] added=[" + TextUtils.join(",", added)
                    + "]";
        }
    }

    /**
     * Generic class to compare two lists of items of type {@code T} whose properties can change.
     * The items to be compared must provide a way to calculate a corresponding key of type
     * {@code K} such that if (and only if) an old and a new item have the same key, then the new
     * item is an update of the old item. Both the old list and the new list may not contain more
     * than one item with the same key, and may not contain any null items.
     *
     * @param <K> A class that represents the key of the items to be compared.
     * @param <T> The class that represents the object to be compared.
     */
    public static class CompareOrUpdateResult<K, T> {
        public final List<T> added = new ArrayList<>();
        public final List<T> removed = new ArrayList<>();
        public final List<T> updated = new ArrayList<>();

        /**
         * Compares two lists of items.
         * @param oldItems the old list of items.
         * @param newItems the new list of items.
         * @param keyCalculator a {@link Function} that calculates an item's key.
         */
        public CompareOrUpdateResult(Collection<T> oldItems, Collection<T> newItems,
                Function<T, K> keyCalculator) {
            HashMap<K, T> updateTracker = new HashMap<>();

            if (oldItems != null) {
                for (T oldItem : oldItems) {
                    updateTracker.put(keyCalculator.apply(oldItem), oldItem);
                }
            }

            if (newItems != null) {
                for (T newItem : newItems) {
                    T oldItem = updateTracker.remove(keyCalculator.apply(newItem));
                    if (oldItem != null) {
                        if (!oldItem.equals(newItem)) {
                            // Update of existing item.
                            updated.add(newItem);
                        }
                    } else {
                        // New item.
                        added.add(newItem);
                    }
                }
            }

            removed.addAll(updateTracker.values());
        }

        @Override
        public String toString() {
            return "removed=[" + TextUtils.join(",", removed)
                    + "] added=[" + TextUtils.join(",", added)
                    + "] updated=[" + TextUtils.join(",", updated)
                    + "]";
        }
    }

    /**
     * Compares the addresses in {@code left} LinkProperties with {@code right}
     * LinkProperties, examining only addresses on the base link.
     *
     * @param left A LinkProperties with the old list of addresses.
     * @param right A LinkProperties with the new list of addresses.
     * @return the differences between the addresses.
     */
    public static @NonNull CompareResult<LinkAddress> compareAddresses(
            @Nullable LinkProperties left, @Nullable LinkProperties right) {
        /*
         * Duplicate the LinkAddresses into removed, we will be removing
         * address which are common between mLinkAddresses and target
         * leaving the addresses that are different. And address which
         * are in target but not in mLinkAddresses are placed in the
         * addedAddresses.
         */
        return new CompareResult<>(left != null ? left.getLinkAddresses() : null,
                right != null ? right.getLinkAddresses() : null);
    }

   /**
     * Compares {@code left} {@code LinkProperties} interface addresses against the {@code right}.
     *
     * @param left A LinkProperties.
     * @param right LinkProperties to be compared with {@code left}.
     * @return {@code true} if both are identical, {@code false} otherwise.
     */
    public static boolean isIdenticalAddresses(@NonNull LinkProperties left,
            @NonNull LinkProperties right) {
        final Collection<InetAddress> leftAddresses = left.getAddresses();
        final Collection<InetAddress> rightAddresses = right.getAddresses();
        return (leftAddresses.size() == rightAddresses.size())
                    ? leftAddresses.containsAll(rightAddresses) : false;
    }

    /**
     * Compares {@code left} {@code LinkProperties} DNS addresses against the {@code right}.
     *
     * @param left A LinkProperties.
     * @param right A LinkProperties to be compared with {@code left}.
     * @return {@code true} if both are identical, {@code false} otherwise.
     */
    public static boolean isIdenticalDnses(@NonNull LinkProperties left,
            @NonNull LinkProperties right) {
        final Collection<InetAddress> leftDnses = left.getDnsServers();
        final Collection<InetAddress> rightDnses = right.getDnsServers();

        final String leftDomains = left.getDomains();
        final String rightDomains = right.getDomains();
        if (leftDomains == null) {
            if (rightDomains != null) return false;
        } else {
            if (!leftDomains.equals(rightDomains)) return false;
        }
        return (leftDnses.size() == rightDnses.size())
                ? leftDnses.containsAll(rightDnses) : false;
    }

    /**
     * Compares {@code left} {@code LinkProperties} HttpProxy against the {@code right}.
     *
     * @param left A LinkProperties.
     * @param right A LinkProperties to be compared with {@code left}.
     * @return {@code true} if both are identical, {@code false} otherwise.
     */
    public static boolean isIdenticalHttpProxy(@NonNull LinkProperties left,
            @NonNull LinkProperties right) {
        return Objects.equals(left.getHttpProxy(), right.getHttpProxy());
    }

    /**
     * Compares {@code left} {@code LinkProperties} interface name against the {@code right}.
     *
     * @param left A LinkProperties.
     * @param right A LinkProperties to be compared with {@code left}.
     * @return {@code true} if both are identical, {@code false} otherwise.
     */
    public static boolean isIdenticalInterfaceName(@NonNull LinkProperties left,
            @NonNull LinkProperties right) {
        return TextUtils.equals(left.getInterfaceName(), right.getInterfaceName());
    }

    /**
     * Compares {@code left} {@code LinkProperties} Routes against the {@code right}.
     *
     * @param left A LinkProperties.
     * @param right A LinkProperties to be compared with {@code left}.
     * @return {@code true} if both are identical, {@code false} otherwise.
     */
    public static boolean isIdenticalRoutes(@NonNull LinkProperties left,
            @NonNull LinkProperties right) {
        final Collection<RouteInfo> leftRoutes = left.getRoutes();
        final Collection<RouteInfo> rightRoutes = right.getRoutes();
        return (leftRoutes.size() == rightRoutes.size())
                ? leftRoutes.containsAll(rightRoutes) : false;
    }
}
