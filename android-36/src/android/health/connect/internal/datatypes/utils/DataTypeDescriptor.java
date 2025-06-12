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

package android.health.connect.internal.datatypes.utils;

import static android.health.connect.datatypes.RecordTypeIdentifier.RECORD_TYPE_UNKNOWN;

import android.annotation.Nullable;
import android.health.connect.HealthDataCategory;
import android.health.connect.HealthPermissionCategory;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.RecordInternal;

import java.util.Objects;

/** @hide */
public class DataTypeDescriptor {
    @RecordTypeIdentifier.RecordType private final int mRecordTypeIdentifier;
    @HealthPermissionCategory.Type private final int mPermissionCategory;
    @HealthDataCategory.Type private final int mDataCategory;
    private final String mReadPermission;
    private final String mWritePermission;
    private final Class<? extends RecordInternal<?>> mRecordInternalClass;
    private final Class<? extends Record> mRecordClass;

    private DataTypeDescriptor(Builder builder) {
        checkArgument(builder.mRecordTypeIdentifier != RECORD_TYPE_UNKNOWN, "Unknown record type");
        checkArgument(
                builder.mHealthPermissionCategory != HealthPermissionCategory.UNKNOWN,
                "Unknown permission category");
        checkArgument(
                builder.mHealthDataCategory != HealthDataCategory.UNKNOWN,
                "Unknown health data category");
        mRecordTypeIdentifier = builder.mRecordTypeIdentifier;
        mPermissionCategory = builder.mHealthPermissionCategory;
        mDataCategory = builder.mHealthDataCategory;
        mReadPermission = Objects.requireNonNull(builder.mReadPermission);
        mWritePermission = Objects.requireNonNull(builder.mWritePermission);
        mRecordInternalClass = Objects.requireNonNull(builder.mRecordInternalClass);
        mRecordClass = Objects.requireNonNull(builder.mRecordClass);
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordTypeIdentifier() {
        return mRecordTypeIdentifier;
    }

    @HealthPermissionCategory.Type
    public int getPermissionCategory() {
        return mPermissionCategory;
    }

    @HealthDataCategory.Type
    public int getDataCategory() {
        return mDataCategory;
    }

    public String getReadPermission() {
        return mReadPermission;
    }

    public String getWritePermission() {
        return mWritePermission;
    }

    public Class<? extends RecordInternal<?>> getRecordInternalClass() {
        return mRecordInternalClass;
    }

    public Class<? extends Record> getRecordClass() {
        return mRecordClass;
    }

    interface RecordTypeIdentifierBuilderStep {
        PermissionCategoryBuilderStep setRecordTypeIdentifier(
                @RecordTypeIdentifier.RecordType int recordTypeIdentifier);
    }

    interface PermissionCategoryBuilderStep {
        DataCategoryBuilderStep setPermissionCategory(
                @HealthPermissionCategory.Type int healthPermissionCategory);
    }

    interface DataCategoryBuilderStep {
        ReadPermissionBuilderStep setDataCategory(@HealthDataCategory.Type int healthDataCategory);
    }

    interface ReadPermissionBuilderStep {
        WritePermissionBuilderStep setReadPermission(String readPermission);
    }

    interface WritePermissionBuilderStep {
        RecordClassBuilderStep setWritePermission(String writePermission);
    }

    interface RecordClassBuilderStep {
        RecordInternalClassBuilderStep setRecordClass(Class<? extends Record> recordClass);
    }

    interface RecordInternalClassBuilderStep {
        BuildStep setRecordInternalClass(Class<? extends RecordInternal<?>> recordInternalClass);
    }

    interface BuildStep {
        DataTypeDescriptor build();
    }

    static RecordTypeIdentifierBuilderStep builder() {
        return new Builder();
    }

    /* Using the step builder pattern to make the builder compile time safe. */
    private static class Builder
            implements RecordTypeIdentifierBuilderStep,
                    PermissionCategoryBuilderStep,
                    DataCategoryBuilderStep,
                    ReadPermissionBuilderStep,
                    WritePermissionBuilderStep,
                    RecordClassBuilderStep,
                    RecordInternalClassBuilderStep,
                    BuildStep {
        @RecordTypeIdentifier.RecordType private int mRecordTypeIdentifier = RECORD_TYPE_UNKNOWN;

        @HealthPermissionCategory.Type
        private int mHealthPermissionCategory = HealthPermissionCategory.UNKNOWN;

        @HealthDataCategory.Type private int mHealthDataCategory = HealthDataCategory.UNKNOWN;
        @Nullable private String mReadPermission;
        @Nullable private String mWritePermission;
        @Nullable private Class<? extends Record> mRecordClass;
        @Nullable private Class<? extends RecordInternal<?>> mRecordInternalClass;

        private Builder() {}

        @Override
        public PermissionCategoryBuilderStep setRecordTypeIdentifier(
                @RecordTypeIdentifier.RecordType int recordTypeIdentifier) {
            checkArgument(
                    recordTypeIdentifier != HealthPermissionCategory.UNKNOWN,
                    "Unknown record type identifier");
            mRecordTypeIdentifier = recordTypeIdentifier;
            return this;
        }

        @Override
        public DataCategoryBuilderStep setPermissionCategory(
                @HealthPermissionCategory.Type int permissionCategory) {
            checkArgument(
                    permissionCategory != HealthPermissionCategory.UNKNOWN,
                    "Unknown permission category");
            mHealthPermissionCategory = permissionCategory;
            return this;
        }

        @Override
        public ReadPermissionBuilderStep setDataCategory(
                @HealthDataCategory.Type int healthDataCategory) {
            checkArgument(
                    healthDataCategory != HealthDataCategory.UNKNOWN,
                    "Unknown health data category");
            mHealthDataCategory = healthDataCategory;
            return this;
        }

        @Override
        public WritePermissionBuilderStep setReadPermission(String readPermission) {
            mReadPermission = Objects.requireNonNull(readPermission);
            return this;
        }

        @Override
        public RecordClassBuilderStep setWritePermission(String writePermission) {
            mWritePermission = Objects.requireNonNull(writePermission);
            return this;
        }

        @Override
        public RecordInternalClassBuilderStep setRecordClass(Class<? extends Record> recordClass) {
            mRecordClass = Objects.requireNonNull(recordClass);
            return this;
        }

        @Override
        public BuildStep setRecordInternalClass(
                Class<? extends RecordInternal<?>> recordInternalClass) {
            mRecordInternalClass = Objects.requireNonNull(recordInternalClass);
            return this;
        }

        @Override
        public DataTypeDescriptor build() {
            return new DataTypeDescriptor(this);
        }
    }

    private static void checkArgument(boolean expression, String errorMsg) {
        if (!expression) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
