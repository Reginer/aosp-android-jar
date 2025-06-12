/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.server.wm.utils;

import android.content.res.TypedArray;
import android.util.ArrayMap;
import android.util.SparseArray;

import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.policy.AttributeCache;

import java.util.function.Function;

/**
 * A wrapper of AttributeCache to preserve more dedicated style caches.
 * @param <T> The type of style cache.
 */
public class WindowStyleCache<T> {
    @GuardedBy("itself")
    private final ArrayMap<String, SparseArray<T>> mCache = new ArrayMap<>();
    private final Function<TypedArray, T> mEntryFactory;

    public WindowStyleCache(Function<TypedArray, T> entryFactory) {
        mEntryFactory = entryFactory;
    }

    /** Returns the cached entry. */
    public T get(String packageName, int theme, int userId) {
        SparseArray<T> themeMap;
        synchronized (mCache) {
            themeMap = mCache.get(packageName);
            if (themeMap != null) {
                T style = themeMap.get(theme);
                if (style != null) {
                    return style;
                }
            }
        }

        final AttributeCache attributeCache = AttributeCache.instance();
        if (attributeCache == null) {
            return null;
        }
        final AttributeCache.Entry ent = attributeCache.get(packageName, theme,
                R.styleable.Window, userId);
        if (ent == null) {
            return null;
        }

        final T style = mEntryFactory.apply(ent.array);
        synchronized (mCache) {
            if (themeMap == null) {
                mCache.put(packageName, themeMap = new SparseArray<>());
            }
            themeMap.put(theme, style);
        }
        return style;
    }

    /** Called when the package is updated or removed. */
    public void invalidatePackage(String packageName) {
        synchronized (mCache) {
            mCache.remove(packageName);
        }
    }
}
