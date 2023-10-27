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

import android.annotation.NonNull;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;
import android.os.Parcel;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Objects;

/**
 * A helper class used to create {@link RecordInternal} objects using its bundle.
 *
 * @hide
 */
public final class ParcelRecordConverter {
    private static volatile ParcelRecordConverter sParcelRecordConverter = null;

    private final Map<Integer, Class<? extends RecordInternal<?>>> mDataTypeClassMap;

    private ParcelRecordConverter() {
        // Add any new data type here to facilitate its conversion.
        mDataTypeClassMap = RecordMapper.getInstance().getRecordIdToInternalRecordClassMap();
    }

    @NonNull
    public static synchronized ParcelRecordConverter getInstance() {
        if (sParcelRecordConverter == null) {
            sParcelRecordConverter = new ParcelRecordConverter();
        }

        return sParcelRecordConverter;
    }

    /** Returns a record for {@code bundle}, assuming it is of type represented by {@code type} */
    @NonNull
    public RecordInternal<?> getRecord(
            @NonNull Parcel parcel, @RecordTypeIdentifier.RecordType int type)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException,
                    InvocationTargetException {
        Class<? extends RecordInternal<?>> recordClass = mDataTypeClassMap.get(type);
        Objects.requireNonNull(recordClass);
        RecordInternal<?> recordInternal = recordClass.getConstructor().newInstance();
        recordInternal.populateUsing(parcel);
        return recordInternal;
    }
}
