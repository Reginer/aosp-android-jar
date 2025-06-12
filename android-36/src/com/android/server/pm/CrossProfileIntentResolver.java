/*
 * Copyright 2014, The Android Open Source Project
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

package com.android.server.pm;

import static com.android.server.pm.CrossProfileIntentFilter.FLAG_IS_PACKAGE_FOR_FILTER;

import android.annotation.NonNull;
import android.content.IntentFilter;

import com.android.server.utils.Snappable;
import com.android.server.utils.SnapshotCache;

import java.util.List;

/**
 * Used to find a list of {@link CrossProfileIntentFilter}s that match an intent.
 */
class CrossProfileIntentResolver
        extends WatchedIntentResolver<CrossProfileIntentFilter, CrossProfileIntentFilter>
        implements Snappable {
    @Override
    protected CrossProfileIntentFilter[] newArray(int size) {
        return new CrossProfileIntentFilter[size];
    }

    @Override
    protected boolean isPackageForFilter(String packageName, CrossProfileIntentFilter filter) {
        return (FLAG_IS_PACKAGE_FOR_FILTER & filter.mFlags) != 0;
    }

    @Override
    protected void sortResults(List<CrossProfileIntentFilter> results) {
        //We don't sort the results
    }

    @Override
    protected IntentFilter getIntentFilter(@NonNull CrossProfileIntentFilter input) {
        return input.getIntentFilter();
    }

    CrossProfileIntentResolver() {
        mSnapshot = makeCache();
    }

    // Take the snapshot of F
    protected CrossProfileIntentFilter snapshot(CrossProfileIntentFilter f) {
        return (f == null) ? null : f.snapshot();
    }

    // Copy constructor used only to create a snapshot.
    private CrossProfileIntentResolver(CrossProfileIntentResolver f) {
        copyFrom(f);
        mSnapshot = new SnapshotCache.Sealed();
    }

    // The cache for snapshots, so they are not rebuilt if the base object has not
    // changed.
    final SnapshotCache<CrossProfileIntentResolver> mSnapshot;

    private SnapshotCache makeCache() {
        return new SnapshotCache<CrossProfileIntentResolver>(this, this) {
            @Override
            public CrossProfileIntentResolver createSnapshot() {
                return new CrossProfileIntentResolver(mSource);
            }};
    }

    /**
     * Return a snapshot of the current object.  The snapshot is a read-only copy suitable
     * for read-only methods.
     * @return A snapshot of the current object.
     */
    public CrossProfileIntentResolver snapshot() {
        return mSnapshot.snapshot();
    }
}
