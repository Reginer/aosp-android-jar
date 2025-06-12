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

package android.health.connect;

import static android.health.connect.datatypes.MedicalResource.validateMedicalResourceType;

import static com.android.healthfitness.flags.Flags.FLAG_PERSONAL_HEALTH_RECORD;

import static java.util.Objects.hash;
import static java.util.Objects.requireNonNull;

import android.annotation.FlaggedApi;
import android.annotation.NonNull;
import android.annotation.SystemApi;
import android.health.connect.datatypes.MedicalDataSource;
import android.health.connect.datatypes.MedicalResource;
import android.health.connect.datatypes.MedicalResource.MedicalResourceType;
import android.health.connect.internal.ParcelUtils;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A class to represent a set of {@link MedicalDataSource}s that contributed data to a certain
 * {@link MedicalResource} type. This is used in the response for {@link
 * HealthConnectManager#queryAllMedicalResourceTypeInfos}.
 *
 * @hide
 */
@SystemApi
@FlaggedApi(FLAG_PERSONAL_HEALTH_RECORD)
public final class MedicalResourceTypeInfo implements Parcelable {
    @MedicalResourceType private final int mMedicalResourceType;
    @NonNull private final Set<MedicalDataSource> mContributingDataSources;

    /**
     * @param medicalResourceType The {@link MedicalResource} type.
     * @param contributingDataSources The contributing {@link MedicalDataSource}s of the {@code
     *     medicalResourceType}.
     */
    public MedicalResourceTypeInfo(
            @MedicalResourceType int medicalResourceType,
            @NonNull Set<MedicalDataSource> contributingDataSources) {
        validateMedicalResourceType(medicalResourceType);
        requireNonNull(contributingDataSources);
        mMedicalResourceType = medicalResourceType;
        mContributingDataSources = contributingDataSources;
    }

    private MedicalResourceTypeInfo(@NonNull Parcel in) {
        requireNonNull(in);
        in = ParcelUtils.getParcelForSharedMemoryIfRequired(in);
        mMedicalResourceType = in.readInt();
        validateMedicalResourceType(mMedicalResourceType);
        List<MedicalDataSource> contributingDataSources = new ArrayList<>();
        in.readParcelableList(
                contributingDataSources,
                MedicalDataSource.class.getClassLoader(),
                MedicalDataSource.class);
        mContributingDataSources = Set.copyOf(contributingDataSources);
    }

    @NonNull
    public static final Creator<MedicalResourceTypeInfo> CREATOR =
            new Creator<>() {
                @Override
                public MedicalResourceTypeInfo createFromParcel(Parcel in) {
                    return new MedicalResourceTypeInfo(in);
                }

                @Override
                public MedicalResourceTypeInfo[] newArray(int size) {
                    return new MedicalResourceTypeInfo[size];
                }
            };

    /** Returns the {@link MedicalResource} type. */
    @MedicalResourceType
    public int getMedicalResourceType() {
        return mMedicalResourceType;
    }

    /** Returns contributing {@link MedicalDataSource}s of the {@code mMedicalResourceType}. */
    @NonNull
    public Set<MedicalDataSource> getContributingDataSources() {
        return mContributingDataSources;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        requireNonNull(dest);
        ParcelUtils.putToRequiredMemory(dest, flags, this::writeToParcelInternal);
    }

    private void writeToParcelInternal(@NonNull Parcel dest) {
        requireNonNull(dest);
        dest.writeInt(mMedicalResourceType);
        dest.writeParcelableList(mContributingDataSources.stream().toList(), 0);
    }

    /** Indicates whether some other object is "equal to" this one. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MedicalResourceTypeInfo that)) return false;
        return getMedicalResourceType() == that.getMedicalResourceType()
                && getContributingDataSources().equals(that.getContributingDataSources());
    }

    /** Returns a hash code value for the object. */
    @Override
    public int hashCode() {
        return hash(getMedicalResourceType(), getContributingDataSources());
    }
}
