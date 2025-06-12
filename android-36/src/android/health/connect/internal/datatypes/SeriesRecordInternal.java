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

package android.health.connect.internal.datatypes;

import android.annotation.NonNull;
import android.health.connect.datatypes.IntervalRecord;

import java.util.Set;

/**
 * Parent class for all the Series type records.
 *
 * <p>U -> Sample type for series record
 *
 * @hide
 */
public abstract class SeriesRecordInternal<T extends IntervalRecord, U>
        extends IntervalRecordInternal<T> {
    @NonNull
    public abstract Set<? extends Sample> getSamples();

    @NonNull
    public abstract SeriesRecordInternal setSamples(Set<? extends Sample> samples);

    /** Base class for the series data stored in {@link SeriesRecordInternal} types */
    public interface Sample {}
}
