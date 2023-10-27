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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.datatypes.validation.ValidationUtils.INTDEF_VALIDATION_ERROR_PREFIX;

import android.annotation.NonNull;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A helper class used to convert internal and external data types.
 *
 * @hide
 */
public final class InternalExternalRecordConverter {
    private static volatile InternalExternalRecordConverter sInternalExternalRecordConverter;

    private final Map<Integer, Class<? extends RecordInternal<?>>>
            mRecordIdToInternalRecordClassMap;
    private final Map<Integer, Class<? extends Record>> mRecordIdToExternalRecordClassMap;

    private InternalExternalRecordConverter() {
        // Add any new data type here to facilitate its conversion.
        mRecordIdToInternalRecordClassMap =
                RecordMapper.getInstance().getRecordIdToInternalRecordClassMap();
        mRecordIdToExternalRecordClassMap =
                RecordMapper.getInstance().getRecordIdToExternalRecordClassMap();
    }

    @NonNull
    public static synchronized InternalExternalRecordConverter getInstance() {
        if (sInternalExternalRecordConverter == null) {
            sInternalExternalRecordConverter = new InternalExternalRecordConverter();
        }

        return sInternalExternalRecordConverter;
    }

    /** Returns a new instance of {@link RecordInternal} for the provided {@code type }. */
    @NonNull
    public RecordInternal<?> newInternalRecord(@RecordTypeIdentifier.RecordType int type) {
        Class<? extends RecordInternal<?>> recordClass =
                mRecordIdToInternalRecordClassMap.get(type);
        Objects.requireNonNull(recordClass);
        RecordInternal<?> recordInternal;
        try {
            recordInternal = recordClass.getConstructor().newInstance();
        } catch (InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }

        return recordInternal;
    }

    /** Returns a record for {@param record} */
    @NonNull
    public List<Record> getExternalRecords(@NonNull List<RecordInternal<?>> recordInternals) {
        List<Record> externalRecordList = new ArrayList<>(recordInternals.size());

        for (RecordInternal<?> recordInternal : recordInternals) {
            try {
            externalRecordList.add(recordInternal.toExternalRecord());
            } catch (IllegalArgumentException illegalArgumentException) {
                if (!illegalArgumentException
                        .getMessage()
                        .contains(INTDEF_VALIDATION_ERROR_PREFIX)) {
                    throw illegalArgumentException;
                }
            }
        }

        return externalRecordList;
    }
}
