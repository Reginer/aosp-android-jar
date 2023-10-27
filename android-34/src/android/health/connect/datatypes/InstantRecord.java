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

package android.health.connect.datatypes;

import android.annotation.NonNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Objects;

/** A record that contains a measurement recorded as an instance . */
public abstract class InstantRecord extends Record {
    private final Instant mTime;
    private final ZoneOffset mZoneOffset;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}
     * @param time Time of this activity
     * @param zoneOffset Zone offset of the user when the activity happened
     * @param skipValidation Boolean flag to skip validation of record values.
     */
    InstantRecord(
            @NonNull Metadata metadata,
            @NonNull Instant time,
            @NonNull ZoneOffset zoneOffset,
            boolean skipValidation) {
        super(metadata);
        Objects.requireNonNull(time);
        Objects.requireNonNull(zoneOffset);
        if (!skipValidation && time.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Record time must not be in the future, record time: "
                            + time
                            + " currentTime: "
                            + Instant.now());
        }
        mTime = time;
        mZoneOffset = zoneOffset;
    }

    /**
     * @return Time of the activity
     */
    @NonNull
    public Instant getTime() {
        return mTime;
    }

    /**
     * @return Zone offset of the user when the activity happened
     */
    @NonNull
    public ZoneOffset getZoneOffset() {
        return mZoneOffset;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (super.equals(object)) {
            InstantRecord other = (InstantRecord) object;
            return this.getTime().toEpochMilli() == other.getTime().toEpochMilli()
                    && this.getZoneOffset().equals(other.getZoneOffset());
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), this.getTime(), this.getZoneOffset());
    }
}
