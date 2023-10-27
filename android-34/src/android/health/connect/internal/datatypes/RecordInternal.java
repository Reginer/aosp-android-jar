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

import static android.health.connect.Constants.DEFAULT_INT;
import static android.health.connect.Constants.DEFAULT_LONG;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.health.connect.datatypes.DataOrigin;
import android.health.connect.datatypes.Device;
import android.health.connect.datatypes.Identifier;
import android.health.connect.datatypes.Metadata;
import android.health.connect.datatypes.Record;
import android.health.connect.datatypes.RecordTypeIdentifier;
import android.os.Parcel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all health connect datatype records.
 *
 * @hide
 */
public abstract class RecordInternal<T extends Record> {
    private final int mRecordIdentifier;
    private UUID mUuid;
    private String mPackageName;
    private String mAppName;
    private long mLastModifiedTime = DEFAULT_LONG;
    private String mClientRecordId;
    private long mClientRecordVersion = DEFAULT_LONG;
    private String mManufacturer;
    private String mModel;
    private int mDeviceType;
    private long mDeviceInfoId = DEFAULT_LONG;
    private long mAppInfoId = DEFAULT_LONG;
    private int mRowId = DEFAULT_INT;

    @Metadata.RecordingMethod private int mRecordingMethod;

    RecordInternal() {
        Identifier annotation = this.getClass().getAnnotation(Identifier.class);
        Objects.requireNonNull(annotation);
        mRecordIdentifier = annotation.recordIdentifier();
    }

    @RecordTypeIdentifier.RecordType
    public int getRecordType() {
        return mRecordIdentifier;
    }

    /**
     * Populates self with the data present in {@code parcel}. Reads should be in the same order as
     * write
     */
    public final void populateUsing(@NonNull Parcel parcel) {
        String uuidString = parcel.readString();
        if (uuidString != null && !uuidString.isEmpty()) {
            mUuid = UUID.fromString(uuidString);
        }
        mPackageName = parcel.readString();
        mAppName = parcel.readString();
        mLastModifiedTime = parcel.readLong();
        mClientRecordId = parcel.readString();
        mClientRecordVersion = parcel.readLong();
        mManufacturer = parcel.readString();
        mModel = parcel.readString();
        mDeviceType = parcel.readInt();
        mRecordingMethod = parcel.readInt();

        populateRecordFrom(parcel);
    }

    /**
     * Populates {@code parcel} with the self information, required to reconstructor this object
     * during IPC
     */
    @NonNull
    public final void writeToParcel(@NonNull Parcel parcel) {
        parcel.writeString(mUuid == null ? "" : mUuid.toString());
        parcel.writeString(mPackageName);
        parcel.writeString(mAppName);
        parcel.writeLong(mLastModifiedTime);
        parcel.writeString(mClientRecordId);
        parcel.writeLong(mClientRecordVersion);
        parcel.writeString(mManufacturer);
        parcel.writeString(mModel);
        parcel.writeInt(mDeviceType);
        parcel.writeInt(mRecordingMethod);

        populateRecordTo(parcel);
    }

    @Nullable
    public UUID getUuid() {
        return mUuid;
    }

    @NonNull
    public RecordInternal<T> setUuid(@Nullable UUID uuid) {
        this.mUuid = uuid;
        return this;
    }

    @NonNull
    public RecordInternal<T> setUuid(@Nullable String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            mUuid = null;
            return this;
        }

        mUuid = UUID.fromString(uuid);
        return this;
    }

    @Nullable
    public String getPackageName() {
        return mPackageName;
    }

    @NonNull
    public RecordInternal<T> setPackageName(@Nullable String packageName) {
        this.mPackageName = packageName;
        return this;
    }

    /** Gets row id of this record. */
    public int getRowId() {
        return mRowId;
    }

    /** Sets the row id for this record. */
    public RecordInternal<T> setRowId(int rowId) {
        mRowId = rowId;
        return this;
    }

    /**
     * Returns an application name associated with this record. Currently, it is used for AppInfo
     * generation when inserting a record. May be {@code null}, in which case the app name may be
     * missing in AppInfo.
     */
    @Nullable
    public String getAppName() {
        return mAppName;
    }

    /** Sets the application name for this record. */
    @NonNull
    public RecordInternal<T> setAppName(@Nullable String appName) {
        mAppName = appName;
        return this;
    }

    public long getLastModifiedTime() {
        return mLastModifiedTime;
    }

    @NonNull
    public RecordInternal<T> setLastModifiedTime(long lastModifiedTime) {
        this.mLastModifiedTime = lastModifiedTime;
        return this;
    }

    @Nullable
    public String getClientRecordId() {
        return mClientRecordId;
    }

    @NonNull
    public RecordInternal<T> setClientRecordId(@Nullable String clientRecordId) {
        this.mClientRecordId = clientRecordId;
        return this;
    }

    public long getClientRecordVersion() {
        return mClientRecordVersion;
    }

    @NonNull
    public RecordInternal<T> setClientRecordVersion(long clientRecordVersion) {
        this.mClientRecordVersion = clientRecordVersion;
        return this;
    }

    @Nullable
    public String getManufacturer() {
        return mManufacturer;
    }

    @NonNull
    public RecordInternal<T> setManufacturer(@Nullable String manufacturer) {
        this.mManufacturer = manufacturer;
        return this;
    }

    @Nullable
    public String getModel() {
        return mModel;
    }

    @NonNull
    public RecordInternal<T> setModel(@Nullable String model) {
        this.mModel = model;
        return this;
    }

    @Device.DeviceType
    public int getDeviceType() {
        return mDeviceType;
    }

    @NonNull
    public RecordInternal<T> setDeviceType(@Device.DeviceType int deviceType) {
        this.mDeviceType = deviceType;
        return this;
    }

    public long getDeviceInfoId() {
        return mDeviceInfoId;
    }

    @NonNull
    public RecordInternal<T> setDeviceInfoId(long deviceInfoId) {
        this.mDeviceInfoId = deviceInfoId;
        return this;
    }

    public long getAppInfoId() {
        return mAppInfoId;
    }

    @NonNull
    public RecordInternal<T> setAppInfoId(long appInfoId) {
        this.mAppInfoId = appInfoId;
        return this;
    }

    /** Returns recording method which indicates how data was recorded for the {@link Record} */
    @Metadata.RecordingMethod
    public int getRecordingMethod() {
        return mRecordingMethod;
    }

    /** Sets Recording method to know how data was recorded for the {@link Record} */
    @NonNull
    public RecordInternal<T> setRecordingMethod(@Metadata.RecordingMethod int recordingMethod) {
        this.mRecordingMethod = recordingMethod;
        return this;
    }

    /** Child class must implement this method and return an external record for this record */
    public abstract T toExternalRecord();

    @NonNull
    Metadata buildMetaData() {
        return new Metadata.Builder()
                .setClientRecordId(getClientRecordId())
                .setClientRecordVersion(getClientRecordVersion())
                .setDataOrigin(new DataOrigin.Builder().setPackageName(getPackageName()).build())
                .setId(getUuid() == null ? null : getUuid().toString())
                .setLastModifiedTime(Instant.ofEpochMilli(getLastModifiedTime()))
                .setRecordingMethod(getRecordingMethod())
                .setDevice(
                        new Device.Builder()
                                .setManufacturer(getManufacturer())
                                .setType(getDeviceType())
                                .setModel(getModel())
                                .build())
                .build();
    }

    /**
     * @return the {@link LocalDate} object of this activity start time.
     */
    public abstract LocalDate getLocalDate();

    /**
     * Populate {@code bundle} with the data required to un-bundle self. This is used suring IPC
     * transmissions
     */
    abstract void populateRecordTo(@NonNull Parcel bundle);

    /**
     * Child class must implement this method and populates itself with the data present in {@code
     * bundle}
     */
    abstract void populateRecordFrom(@NonNull Parcel bundle);
}
