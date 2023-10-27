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

import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.internal.datatypes.RecordInternal;

import java.util.Objects;

/** A base class for all record classes */
public abstract class Record {

    private final Metadata mMetadata;
    @RecordTypeIdentifier.RecordType private final int mRecordIdentifier;

    /**
     * @param metadata Metadata to be associated with the record. See {@link Metadata}
     */
    Record(@NonNull Metadata metadata) {
        Objects.requireNonNull(metadata);
        Identifier annotation = getClass().getAnnotation(Identifier.class);
        Objects.requireNonNull(annotation);
        validateIntDefValue(
                annotation.recordIdentifier(),
                RecordTypeIdentifier.VALID_TYPES,
                RecordTypeIdentifier.class.getSimpleName());
        mRecordIdentifier = annotation.recordIdentifier();
        mMetadata = metadata;
    }

    /**
     * @return {@link Metadata} for this record
     */
    @NonNull
    public Metadata getMetadata() {
        return mMetadata;
    }

    /** @hide */
    @SystemApi
    @RecordTypeIdentifier.RecordType
    public int getRecordType() {
        return mRecordIdentifier;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param object the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     */
    @Override
    public boolean equals(@NonNull Object object) {
        if (this == object) return true;
        if (Objects.isNull(object)) {
            return false;
        }

        if (getClass().isAssignableFrom(object.getClass())) {
            Record other = (Record) object;
            return getMetadata().equals(other.getMetadata())
                    && getRecordType() == other.getRecordType();
        }
        return false;
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return Objects.hash(getMetadata(), getRecordType());
    }

    /** @hide */
    public abstract RecordInternal<?> toRecordInternal();
}
