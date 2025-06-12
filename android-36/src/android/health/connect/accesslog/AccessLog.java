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

package android.health.connect.accesslog;

import static android.health.connect.datatypes.MedicalResource.validateMedicalResourceType;
import static android.health.connect.datatypes.validation.ValidationUtils.validateIntDefValue;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;
import static com.android.healthfitness.flags.Flags.personalHealthRecord;

import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.IntDef;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.Constants;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.health.connect.internal.datatypes.utils.HealthConnectMappings;
import android.os.Parcel;
import android.os.Parcelable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A class to represent access log which is logged whenever a package requests a read on a record
 * type
 *
 * @hide
 */
@SystemApi
public final class AccessLog implements Parcelable {
    @NonNull private final String mPackageName;
    @NonNull private final Instant mAccessTime;
    @OperationType.OperationTypes private final int mOperationType;
    @NonNull private final List<Class<? extends Record>> mRecordTypesList = new ArrayList<>();
    @NonNull @MedicalResourceType private Set<Integer> mMedicalResourceTypes = new HashSet<>();
    private boolean mIsMedicalDataSourceAccessed = false;

    /**
     * Creates an access logs object that can be used to get access log request for {@code
     * packageName}
     *
     * @param packageName name of the package that requested an access
     * @param recordTypes List of Record class type the was accessed
     * @param accessTimeInMillis time when the access was requested
     * @param operationType Type of access
     * @hide
     */
    public AccessLog(
            @NonNull String packageName,
            @NonNull @RecordTypeIdentifier.RecordType List<Integer> recordTypes,
            long accessTimeInMillis,
            @OperationType.OperationTypes int operationType) {
        requireNonNull(packageName);
        requireNonNull(recordTypes);

        mPackageName = packageName;
        HealthConnectMappings healthConnectMappings = HealthConnectMappings.getInstance();
        for (@RecordTypeIdentifier.RecordType int recordType : recordTypes) {
            mRecordTypesList.add(
                    healthConnectMappings.getRecordIdToExternalRecordClassMap().get(recordType));
        }
        mAccessTime = Instant.ofEpochMilli(accessTimeInMillis);
        mOperationType = operationType;
    }

    /**
     * Creates an access logs object that can be used to get access log request for {@code
     * packageName}
     *
     * @param packageName name of the package that requested an access
     * @param accessTimeInMillis time when the access was requested
     * @param operationType Type of access
     * @param medicalResourceTypes Set of {@link MedicalResourceType}s that was accessed by the app
     * @param isMedicalDataSourceAccessed Whether or not any {@link MedicalDataSource}s was accessed
     * @hide
     */
    public AccessLog(
            @NonNull String packageName,
            long accessTimeInMillis,
            @OperationType.OperationTypes int operationType,
            @NonNull @MedicalResourceType Set<Integer> medicalResourceTypes,
            boolean isMedicalDataSourceAccessed) {
        if (!personalHealthRecord()) {
            throw new UnsupportedOperationException(
                    "Constructing AccessLog for medical data is not supported");
        }
        requireNonNull(packageName);
        OperationType.validateOperationType(operationType);
        requireNonNull(medicalResourceTypes);
        for (@MedicalResourceType int medicalResourceType : medicalResourceTypes) {
            validateMedicalResourceType(medicalResourceType);
        }
        mPackageName = packageName;
        mAccessTime = Instant.ofEpochMilli(accessTimeInMillis);
        mOperationType = operationType;
        mMedicalResourceTypes = medicalResourceTypes;
        mIsMedicalDataSourceAccessed = isMedicalDataSourceAccessed;
    }

    private AccessLog(Parcel in) {
        HealthConnectMappings healthConnectMappings = HealthConnectMappings.getInstance();

        int[] recordTypes = requireNonNull(in.createIntArray());
        for (@RecordTypeIdentifier.RecordType int recordType : recordTypes) {
            mRecordTypesList.add(
                    healthConnectMappings.getRecordIdToExternalRecordClassMap().get(recordType));
        }
        mPackageName = requireNonNull(in.readString());
        mAccessTime = Instant.ofEpochMilli(in.readLong());
        mOperationType = in.readInt();
        if (personalHealthRecord()) {
            int[] medicalResourceTypes = requireNonNull(in.createIntArray());
            for (@MedicalResourceType int medicalResourceType : medicalResourceTypes) {
                mMedicalResourceTypes.add(medicalResourceType);
            }
            mIsMedicalDataSourceAccessed = in.readBoolean();
        }
    }

    @NonNull
    public static final Creator<AccessLog> CREATOR =
            new Creator<>() {
                @Override
                public AccessLog createFromParcel(Parcel in) {
                    return new AccessLog(in);
                }

                @Override
                public AccessLog[] newArray(int size) {
                    return new AccessLog[size];
                }
            };

    /** Returns List of Record types that was accessed by the app */
    @NonNull
    public List<Class<? extends Record>> getRecordTypes() {
        return mRecordTypesList;
    }

    /** Returns package name of app that accessed the records */
    @NonNull
    public String getPackageName() {
        return mPackageName;
    }

    /** Returns the instant at which the app accessed the record */
    @NonNull
    public Instant getAccessTime() {
        return mAccessTime;
    }

    /** Returns the type of operation performed by the app */
    @OperationType.OperationTypes
    public int getOperationType() {
        return mOperationType;
    }

    /** Returns Set of {@link MedicalResourceType}s that was accessed by the app */
    @NonNull
    @MedicalResourceType
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public Set<Integer> getMedicalResourceTypes() {
        return mMedicalResourceTypes;
    }

    /** Returns whether or not any {@link MedicalDataSource}s was accessed by the app */
    @FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
    public boolean isMedicalDataSourceAccessed() {
        return mIsMedicalDataSourceAccessed;
    }

    /** Identifier for Operation type. */
    public static final class OperationType {

        /** Identifier for read operation done on user health data. */
        public static final int OPERATION_TYPE_READ = Constants.READ;

        /** Identifier for update or insert operation done on user health data. */
        public static final int OPERATION_TYPE_UPSERT = Constants.UPSERT;

        /** Identifier for delete operation done on user health data. */
        public static final int OPERATION_TYPE_DELETE = Constants.DELETE;

        /** @hide */
        @IntDef({OPERATION_TYPE_UPSERT, OPERATION_TYPE_DELETE, OPERATION_TYPE_READ})
        @Retention(RetentionPolicy.SOURCE)
        public @interface OperationTypes {}

        /**
         * Validates the provided {@code operationType} is in the valid set.
         *
         * <p>Throws {@link IllegalArgumentException} if not.
         */
        private static void validateOperationType(@OperationTypes int operationType) {
            validateIntDefValue(
                    operationType,
                    Set.of(OPERATION_TYPE_UPSERT, OPERATION_TYPE_DELETE, OPERATION_TYPE_READ),
                    OperationTypes.class.getSimpleName());
        }

        private OperationType() {}
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Flatten this object in to a Parcel.
     *
     * @param dest The Parcel in which the object should be written.
     * @param flags Additional flags about how the object should be written. May be 0 or {@link
     *     #PARCELABLE_WRITE_RETURN_VALUE}.
     */
    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        int recordTypeCount = mRecordTypesList.size();
        HealthConnectMappings healthConnectMappings = HealthConnectMappings.getInstance();
        @RecordTypeIdentifier.RecordType int[] recordTypes = new int[recordTypeCount];
        for (int i = 0; i < recordTypeCount; i++) {
            recordTypes[i] = healthConnectMappings.getRecordType(mRecordTypesList.get(i));
        }
        dest.writeIntArray(recordTypes);
        dest.writeString(mPackageName);
        dest.writeLong(mAccessTime.toEpochMilli());
        dest.writeInt(mOperationType);
        if (personalHealthRecord()) {
            dest.writeIntArray(
                    mMedicalResourceTypes.stream().mapToInt(Integer::intValue).toArray());
            dest.writeBoolean(mIsMedicalDataSourceAccessed);
        }
    }
}
